package br.com.tokenizedbikes.flows.biketoken

import br.com.tokenizedbikes.flows.accounts.GetAccountPubKeyAndEncapsulate
import br.com.tokenizedbikes.flows.biketoken.models.BikeCommercializationFlowResponse
import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
import br.com.tokenizedbikes.states.BikeTokenState
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlow
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlowHandler
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.util.stream.Collectors

object BikeSaleFlow {

    @StartableByRPC
    @InitiatingFlow
    class BikeSaleInitiatingFlow(
        private val bikeSerialNumber: String,
        private val paymentTokenIdentifier: String,
        private val fractionDigits: Int,
        private val sellerAccount: AccountInfo,
        private val buyerAccount: AccountInfo
    ) : FlowLogic<BikeCommercializationFlowResponse>() {

        companion object {
            object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Bike Sale Transaction.")
            object VERYFYING_BIKE_TOKEN : ProgressTracker.Step("Verifying if node has Bike Token reference.")
            object INITIATING_SAME_NODE_WORKFLOW : ProgressTracker.Step("Initiating Buyer and Seller same node workflow.")
            object GETTING_BUYER_REFERENCE : ProgressTracker.Step("Getting Buyer reference.")
            object GETTING_BIKE_REF_BUYER: ProgressTracker.Step("Getting Bike Reference States from Buyer.")
            object MOVING_TOKENS: ProgressTracker.Step("Moving Tokens.")
            object SIGING_TRANSACTION: ProgressTracker.Step("Signing transaction.")
            object GETTING_MISSING_KEYS_REF: ProgressTracker.Step("Getting Missing Keys Reference.")
            object FINALIZING_TRANSACTION: ProgressTracker.Step("Finalizing transaction.")
            object GENERATING_MOVE : ProgressTracker.Step("Calling Move Flow.")

            fun tracker() = ProgressTracker(
                INITIATING_TRANSACTION,
                VERYFYING_BIKE_TOKEN,
                INITIATING_SAME_NODE_WORKFLOW,
                GETTING_BUYER_REFERENCE,
                GETTING_BIKE_REF_BUYER,
                MOVING_TOKENS,
                SIGING_TRANSACTION,
                GETTING_MISSING_KEYS_REF,
                FINALIZING_TRANSACTION,
                GENERATING_MOVE
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): BikeCommercializationFlowResponse {
            progressTracker.currentStep = INITIATING_TRANSACTION

            requireThat { "Seller account host should match the host node" using(sellerAccount.host == ourIdentity) }

            progressTracker.currentStep = VERYFYING_BIKE_TOKEN

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val vaultCustomQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

            val vaultPage = vaultCustomQueryService.getBikeTokenBySerialNumber(serialNumber = bikeSerialNumber)

            if (vaultPage.states.isEmpty())
                throw FlowException("No states with 'serialNumber' - $bikeSerialNumber found")

            val bikeStateAndRef = vaultPage.states.single()

            val bikeState = bikeStateAndRef.state.data

            val txBuilder = TransactionBuilder(notary = notary)

            val bikeTokenSelectionCriteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED,
                externalIds = listOf(sellerAccount.identifier.id)
            )

            val stx: SignedTransaction;
            
            if (buyerAccount.host == ourIdentity) {
                progressTracker.currentStep = INITIATING_SAME_NODE_WORKFLOW

                stx = BikeSaleFlowWithinNode(
                    txBuilder = txBuilder,
                    bikeState = bikeState,
                    bikeTokenSelectionCriteria = bikeTokenSelectionCriteria
                ).call()
            } else {
                val bikeStatePointer = bikeState.toPointer(bikeState.javaClass)

                progressTracker.currentStep = GETTING_BUYER_REFERENCE

                val buyerParty = subFlow(GetAccountPubKeyAndEncapsulate(buyerAccount))

                val buyerSession = initiateFlow(buyerAccount.host)

                val tokenIdentifier = TokenType(paymentTokenIdentifier, fractionDigits)

                val amountOfBikeCoins = bikeState.coinPrice of tokenIdentifier

                val sellerParty = serviceHub.createKeyForAccount(sellerAccount)

                val tradeInfo = TradeInfo(
                    amountOfCoins = amountOfBikeCoins,
                    buyerParty = buyerParty,
                    sellerParty = sellerParty
                )

                buyerSession.send(tradeInfo)

                progressTracker.currentStep = GETTING_BIKE_REF_BUYER

                val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(buyerSession))

                val moneyReceived: List<FungibleToken> =
                    buyerSession.receive<List<FungibleToken>>().unwrap { it }

                progressTracker.currentStep = MOVING_TOKENS

                addMoveNonFungibleTokens(
                    txBuilder,
                    serviceHub,
                    bikeStatePointer,
                    buyerParty,
                    bikeTokenSelectionCriteria
                )

                addMoveTokens(txBuilder, inputs, moneyReceived)

                val signers =
                    txBuilder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub) + ourIdentity.owningKey

                progressTracker.currentStep = GETTING_MISSING_KEYS_REF

                /* Mapping Missing keys */
                val missingKeys = inputs.map {
                    it.state.data.holder
                }.distinct().filter {
                    serviceHub.identityService.wellKnownPartyFromAnonymous(
                        it
                    ) == null
                }

                buyerSession.send(missingKeys)
                subFlow(SyncKeyMappingFlowHandler(buyerSession))
                /* Mapping Missing Keys */

                progressTracker.currentStep = SIGING_TRANSACTION

                val initialSignedTrnx = serviceHub.signInitialTransaction(txBuilder, signers)

                val ftx = subFlow(CollectSignaturesFlow(initialSignedTrnx, listOf(buyerSession)))

                progressTracker.currentStep = FINALIZING_TRANSACTION

                stx = subFlow(FinalityFlow(ftx, listOf(buyerSession)))
            }

            subFlow(UpdateDistributionListFlow(stx))

            return BikeCommercializationFlowResponse(
                txId = stx.id.toHexString(),
                bikeSerialNumber = bikeSerialNumber,
                buyer = buyerAccount,
                seller = sellerAccount,
                priceInCoins = bikeState.coinPrice,
                bikeTokenLinearId = bikeState.linearId
            )
        }

        inner class BikeSaleFlowWithinNode(
            private val txBuilder: TransactionBuilder,
            private val bikeState: BikeTokenState,
            private val bikeTokenSelectionCriteria: QueryCriteria.VaultQueryCriteria) {

            @Suspendable
            fun call(): SignedTransaction {
                val bikeStatePointer = bikeState.toPointer(bikeState.javaClass)

                val tokenIdentifier = TokenType(paymentTokenIdentifier, fractionDigits)

                val amountOfBikeCoins = bikeState.coinPrice of tokenIdentifier

                val sellerParty = serviceHub.createKeyForAccount(sellerAccount)

                val buyerParty = serviceHub.createKeyForAccount(buyerAccount)

                val buyerCoinCriteria = QueryCriteria.VaultQueryCriteria(
                    status = Vault.StateStatus.UNCONSUMED,
                    externalIds = listOf(buyerAccount.identifier.id)
                )

                progressTracker.currentStep = MOVING_TOKENS

                addMoveFungibleTokens(txBuilder, serviceHub, amountOfBikeCoins, sellerParty, buyerParty, buyerCoinCriteria)

                addMoveNonFungibleTokens(
                    txBuilder,
                    serviceHub,
                    bikeStatePointer,
                    buyerParty,
                    bikeTokenSelectionCriteria
                )

                val signers =
                    txBuilder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub) + ourIdentity.owningKey

                progressTracker.currentStep = SIGING_TRANSACTION

                val selfSignedTransaction =  serviceHub.signInitialTransaction(txBuilder, signers)

                progressTracker.currentStep = FINALIZING_TRANSACTION

                subFlow(ObserverAwareFinalityFlow(selfSignedTransaction, emptyList()))

                return selfSignedTransaction
            }
        }

    }

    @InitiatedBy(BikeSaleInitiatingFlow::class)
    class BikeSaleResponderFlow(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val tradeInfo = counterpartySession.receive<TradeInfo>().unwrap { it }

            val accountInfo = serviceHub.accountService.accountInfo(tradeInfo.buyerParty.owningKey)?.state?.data

            requireThat { "Buyer account doesn't exists on node" using(accountInfo != null) }

            val bikeCoinSelectionCriteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED,
                externalIds = listOf(accountInfo!!.identifier.id)
            )

            val priceToken = tradeInfo.amountOfCoins

            val inputsAndOutputs: Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> =
                DatabaseTokenSelection(serviceHub).generateMove(
                    listOf(
                        Pair(
                            tradeInfo.sellerParty,
                            priceToken
                        )
                    ), tradeInfo.buyerParty, TokenQueryBy(queryCriteria = bikeCoinSelectionCriteria)
                )

            subFlow(SendStateAndRefFlow(counterpartySession, inputsAndOutputs.first))

            counterpartySession.send(inputsAndOutputs.second)

            /* Mapping Missing keys */
            val potentiallyMissingKeys = inputsAndOutputs.first.stream()
                .map { (state) -> state.data.holder }
                .collect(Collectors.toSet())

            val missingKeys = counterpartySession.receive<List<AbstractParty>>().unwrap { it }

            if (!potentiallyMissingKeys.containsAll(missingKeys)) throw FlowException("A missing key is not in the potentially missing keys")

            subFlow(SyncKeyMappingFlow(counterpartySession, missingKeys))
            /* Mapping Missing keys */

            subFlow(object : SignTransactionFlow(counterpartySession) {
                @Throws(FlowException::class)
                override fun checkTransaction(stx: SignedTransaction) { // Custom Logic to validate transaction.
                }
            })
            return subFlow(ReceiveFinalityFlow(counterpartySession))
        }

    }

    @CordaSerializable
    data class TradeInfo(
        val amountOfCoins: Amount<TokenType>,
        val sellerParty: AnonymousParty,
        val buyerParty: AnonymousParty
    )

}