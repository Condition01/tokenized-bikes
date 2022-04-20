package br.com.tokenizedbikes.flows.biketoken

import br.com.tokenizedbikes.flows.accounts.GetAccountPubKeyAndEncapsulate
import br.com.tokenizedbikes.flows.biketoken.models.BikeCommercializationFlowResponse
import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlow
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlowHandler
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
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
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap
import java.util.function.Function
import java.util.function.Predicate
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

        @Suspendable
        override fun call(): BikeCommercializationFlowResponse {
            requireThat { "Seller account doesn't exist in this node" using(sellerAccount.host == ourIdentity) }

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val vaultCustomQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

            val vaultPage = vaultCustomQueryService.getBikeTokenBySerialNumber(serialNumber = bikeSerialNumber)

            if (vaultPage.states.isEmpty())
                throw FlowException("No states with 'serialNumber' - $bikeSerialNumber found")

            val bikeStateAndRef = vaultPage.states.single()

            val bikeState = bikeStateAndRef.state.data

            val bikeStatePointer = bikeState.toPointer(bikeState.javaClass)

            val txBuilder = TransactionBuilder(notary = notary)

            val bikeTokenSelectionCriteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED,
                externalIds = listOf(sellerAccount.identifier.id)
            )

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

            val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(buyerSession))

            val moneyReceived: List<FungibleToken> =
                buyerSession.receive<List<FungibleToken>>().unwrap { it }

            addMoveNonFungibleTokens(txBuilder, serviceHub, bikeStatePointer, buyerParty, bikeTokenSelectionCriteria)

            addMoveTokens(txBuilder, inputs, moneyReceived)

            val signers = txBuilder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub) + ourIdentity.owningKey

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

            val initialSignedTrnx = serviceHub.signInitialTransaction(txBuilder, signers)

            val ftx = subFlow(CollectSignaturesFlow(initialSignedTrnx, listOf(buyerSession)))

            val stx = subFlow(FinalityFlow(ftx, listOf(buyerSession)))

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

    }

    @InitiatedBy(BikeSaleInitiatingFlow::class)
    class BikeSaleResponderFlow(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val tradeInfo = counterpartySession.receive<TradeInfo>().unwrap { it }

            val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

            val accountInfo = accountService.accountInfo(tradeInfo.buyerParty.owningKey)?.state?.data

            requireThat { "Buyer account doesn't exist in this node" using(accountInfo != null) }

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