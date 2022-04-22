package br.com.tokenizedbikes.flows.biketoken

import br.com.tokenizedbikes.flows.accounts.GetAccountPubKeyAndEncapsulate
import br.com.tokenizedbikes.flows.biketoken.models.BikeCommercializationFlowResponse
import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
import br.com.tokenizedbikes.states.BikeTokenState
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

object BikePurchaseFlow {

    @StartableByRPC
    @InitiatingFlow
    class BikePurchaseInitiatingFlow(
        val bikeSerialNumber: String,
        val tokenIdentifierString: String,
        val fractionDigits: Int,
        val sellerAccount: AccountInfo,
        val buyerAccount: AccountInfo
    ) : FlowLogic<BikeCommercializationFlowResponse>() {

        companion object {
            object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Bike Purchase Transaction.")
            object INITIATING_SAME_NODE_WORKFLOW : ProgressTracker.Step("Initiating Buyer and Seller same node workflow.")
            object GETTING_SELLER_REFERENCE : ProgressTracker.Step("Getting Seller reference.")
            object GETTING_BIKE_PRICE_SELLER: ProgressTracker.Step("Getting Bike Price from Seller.")
            object MOVING_TOKENS: ProgressTracker.Step("Moving Tokens.")
            object SIGING_TRANSACTION: ProgressTracker.Step("Signing transaction.")
            object FINALIZING_TRANSACTION: ProgressTracker.Step("Finalizing transaction.")
            object GENERATING_MOVE : ProgressTracker.Step("Calling Move Flow.")

            fun tracker() = ProgressTracker(
                INITIATING_TRANSACTION,
                INITIATING_SAME_NODE_WORKFLOW,
                GETTING_SELLER_REFERENCE,
                GETTING_BIKE_PRICE_SELLER,
                MOVING_TOKENS,
                SIGING_TRANSACTION,
                FINALIZING_TRANSACTION,
                GENERATING_MOVE
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): BikeCommercializationFlowResponse {
            progressTracker.currentStep = INITIATING_TRANSACTION

            requireThat { "The buyer account host should match the node host" using (buyerAccount.host == ourIdentity) }

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val txBuilder = TransactionBuilder(notary = notary)

            val tokenIdentifier = TokenType(tokenIdentifierString, fractionDigits)

            if (sellerAccount.host == ourIdentity) {
                progressTracker.currentStep = INITIATING_SAME_NODE_WORKFLOW

                return BikePurchaseFlowWithinNode(
                    txBuilder = txBuilder,
                    tokenIdentifier = tokenIdentifier
                ).call()
            }

            val sellerSession = initiateFlow(sellerAccount.host)

            progressTracker.currentStep = GETTING_SELLER_REFERENCE

            val sellerParty = subFlow(GetAccountPubKeyAndEncapsulate(sellerAccount))

            val tradeInfo = TradeInfo(
                bikeSerialNumber = bikeSerialNumber,
                sellerParty = sellerParty,
                buyerAccount = buyerAccount
            )

            sellerSession.send(tradeInfo)

            progressTracker.currentStep = GETTING_BIKE_PRICE_SELLER

            val bikePriceAndId: Pair<Double, UniqueIdentifier> =
                sellerSession.receive<Pair<Double, UniqueIdentifier>>().unwrap { it }

            val amountOfBikeCoins = bikePriceAndId.first of tokenIdentifier

            /*** You can use this method - work same way as 'addMoveTokens' below
            addMoveFungibleTokens(txBuilder, serviceHub, amountOfBikeCoins, seller, ourIdentity)
             */

            val bikeTokenSelectionCriteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED,
                externalIds = listOf(buyerAccount.identifier.id)
            )

            val buyerParty = serviceHub.createKeyForAccount(buyerAccount)

            progressTracker.currentStep = MOVING_TOKENS

            val inputsAndOutputs: Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> =
                DatabaseTokenSelection(serviceHub).generateMove(
                    listOf(
                        Pair(
                            sellerParty,
                            amountOfBikeCoins
                        )
                    ), buyerParty, TokenQueryBy(queryCriteria = bikeTokenSelectionCriteria)
                )

            addMoveTokens(txBuilder, inputsAndOutputs.first, inputsAndOutputs.second)

            val signers =
                txBuilder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub) + ourIdentity.owningKey

            progressTracker.currentStep = SIGING_TRANSACTION

            val ftx = serviceHub.signInitialTransaction(txBuilder, signers)

            progressTracker.currentStep = FINALIZING_TRANSACTION

            val stx = subFlow(FinalityFlow(ftx, listOf(sellerSession)))

            subFlow(UpdateDistributionListFlow(stx))

            return BikeCommercializationFlowResponse(
                txId = stx.id.toHexString(),
                bikeSerialNumber = bikeSerialNumber,
                buyer = buyerAccount,
                seller = sellerAccount,
                priceInCoins = bikePriceAndId.first,
                bikeTokenLinearId = bikePriceAndId.second
            )
        }

        inner class BikePurchaseFlowWithinNode(
            private val txBuilder: TransactionBuilder,
            private val tokenIdentifier: TokenType
        ) {
            @Suspendable
            fun call(): BikeCommercializationFlowResponse {
                val sellerParty = serviceHub.createKeyForAccount(sellerAccount)

                val buyerParty = serviceHub.createKeyForAccount(buyerAccount)

                val vaultBikeTokenQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

                val vaultPage = vaultBikeTokenQueryService.getBikeTokenBySerialNumber(
                    serialNumber = bikeSerialNumber
                )

                if (vaultPage.states.isEmpty())
                    throw FlowException("No states with 'serialNumber' - $bikeSerialNumber found")

                val bikeStateAndRef = vaultPage.states.single()

                val bikeStatePointer = bikeStateAndRef.state.data.toPointer<BikeTokenState>()

                val amountOfBikeCoins = bikeStateAndRef.state.data.coinPrice of tokenIdentifier

                val sellerTokenCriteria = QueryCriteria.VaultQueryCriteria(
                    status = Vault.StateStatus.UNCONSUMED,
                    externalIds = listOf(sellerAccount.identifier.id)
                )

                progressTracker.currentStep = MOVING_TOKENS

                addMoveNonFungibleTokens(
                    txBuilder,
                    serviceHub,
                    bikeStatePointer,
                    buyerParty,
                    sellerTokenCriteria
                )

                val buyerCoinCriteria = QueryCriteria.VaultQueryCriteria(
                    status = Vault.StateStatus.UNCONSUMED,
                    externalIds = listOf(buyerAccount.identifier.id)
                )

                addMoveFungibleTokens(
                    txBuilder,
                    serviceHub,
                    amountOfBikeCoins,
                    sellerParty,
                    buyerParty,
                    buyerCoinCriteria)

                val signers =
                    txBuilder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub) + ourIdentity.owningKey

                progressTracker.currentStep = SIGING_TRANSACTION

                val selfSignedTransaction = serviceHub.signInitialTransaction(txBuilder, signers)

                progressTracker.currentStep = FINALIZING_TRANSACTION

                subFlow(ObserverAwareFinalityFlow(selfSignedTransaction, emptyList()))

                subFlow(UpdateDistributionListFlow(selfSignedTransaction))

                return BikeCommercializationFlowResponse(
                    txId = selfSignedTransaction.id.toHexString(),
                    bikeSerialNumber = bikeSerialNumber,
                    buyer = buyerAccount,
                    seller = sellerAccount,
                    priceInCoins = bikeStateAndRef.state.data.coinPrice,
                    bikeTokenLinearId = bikeStateAndRef.state.data.linearId
                )
            }
        }

    }

    @InitiatedBy(BikePurchaseInitiatingFlow::class)
    class BikePurchaseResponderFlow(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val tradeInfo = counterpartySession.receive<TradeInfo>().unwrap { it }

            val buyerParty = subFlow(GetAccountPubKeyAndEncapsulate(tradeInfo.buyerAccount))

            val accountInfo = serviceHub.accountService.accountInfo(tradeInfo.sellerParty.owningKey)?.state?.data

            requireThat { "Seller account doesn't exists on node" using (accountInfo != null) }

            val bikeTokenSelectionCriteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED,
                externalIds = listOf(accountInfo!!.identifier.id)
            )

            val vaultBikeTokenQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

            val vaultPage = vaultBikeTokenQueryService.getBikeTokenBySerialNumber(
                serialNumber = tradeInfo.bikeSerialNumber
            )

            if (vaultPage.states.isEmpty())
                throw FlowException("No states with 'serialNumber' - ${tradeInfo.bikeSerialNumber} found")

            val bikeStateAndRef = vaultPage.states.single()

            val bikeStatePointer = bikeStateAndRef.state.data.toPointer<BikeTokenState>()

            val partyAndToken = PartyAndToken(buyerParty, bikeStatePointer)

            counterpartySession.send(
                Pair
                    (bikeStateAndRef.state.data.coinPrice, bikeStateAndRef.state.data.linearId)
            )

            subFlow(ReceiveFinalityFlow(counterpartySession))

            return subFlow(MoveNonFungibleTokens(partyAndToken, listOf(), bikeTokenSelectionCriteria))
        }

    }

    @CordaSerializable
    data class TradeInfo(
        val bikeSerialNumber: String,
        val sellerParty: AnonymousParty,
        val buyerAccount: AccountInfo
    )

}