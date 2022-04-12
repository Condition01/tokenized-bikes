package br.com.tokenizedbikes.flows.biketoken

import br.com.tokenizedbikes.flows.biketoken.models.BikePurchaseFlowResponse
import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

object BikeSaleFlow {

    @StartableByRPC
    @InitiatingFlow
    class BikeSaleInitiatingFlow(
        val bikeSerialNumber: String,
        val paymentTokenIdentifier: String,
        val fractionDigits: Int,
        val buyer: Party
    ) : FlowLogic<BikePurchaseFlowResponse>() {

        @Suspendable
        override fun call(): BikePurchaseFlowResponse {

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val vaultCustomQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

            val vaultPage = vaultCustomQueryService.getBikeTokenBySerialNumber(serialNumber = bikeSerialNumber)

            if (vaultPage.states.isEmpty())
                throw FlowException("No states with 'serialNumber' - $bikeSerialNumber found")

            val bikeStateAndRef = vaultPage.states.single()

            val bikeState = bikeStateAndRef.state.data

            val bikeStatePointer = bikeState.toPointer(bikeState.javaClass)

            val txBuilder = TransactionBuilder(notary = notary)

            addMoveNonFungibleTokens(txBuilder, serviceHub, bikeStatePointer, buyer)

            val buyerSession = initiateFlow(buyer)

            val tokenIdentifier = TokenType(paymentTokenIdentifier, fractionDigits)

            val amountOfBikeCoins = bikeState.coinPrice of tokenIdentifier

            buyerSession.send(amountOfBikeCoins)

            val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(buyerSession))

            val moneyReceived: List<FungibleToken> = buyerSession.receive<List<FungibleToken>>().unwrap { it -> it }

            addMoveTokens(txBuilder, inputs, moneyReceived)

            val initialSignedTrnx = serviceHub.signInitialTransaction(txBuilder)

            val ftx = subFlow(CollectSignaturesFlow(initialSignedTrnx, listOf(buyerSession)))

            val stx = subFlow(FinalityFlow(ftx, listOf(buyerSession)))

            subFlow(UpdateDistributionListFlow(stx))

            return BikePurchaseFlowResponse(
                txId = stx.id.toHexString(),
                bikeSerialNumber = bikeSerialNumber,
                buyer = buyer,
                seller = ourIdentity,
                priceInCoins = bikeState.coinPrice
            )
        }

    }

    @InitiatedBy(BikeSaleInitiatingFlow::class)
    class BikeSaleResponderFlow(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val priceToken = counterpartySession.receive<Amount<TokenType>>().unwrap { it }

            val inputsAndOutputs: Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> =
                DatabaseTokenSelection(serviceHub).generateMove(
                    listOf(
                        Pair(
                            counterpartySession.counterparty,
                            priceToken
                        )
                    ), ourIdentity
                )

            subFlow(SendStateAndRefFlow(counterpartySession, inputsAndOutputs.first))

            counterpartySession.send(inputsAndOutputs.second)

            subFlow(object : SignTransactionFlow(counterpartySession) {
                @Throws(FlowException::class)
                override fun checkTransaction(stx: SignedTransaction) { // Custom Logic to validate transaction.
                }
            })
            return subFlow(ReceiveFinalityFlow(counterpartySession))
        }

    }

}