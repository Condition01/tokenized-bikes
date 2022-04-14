package br.com.tokenizedbikes.flows.biketoken

import br.com.tokenizedbikes.flows.biketoken.models.BikeCommercializationFlowResponse
import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
import br.com.tokenizedbikes.states.BikeTokenState
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

object BikePurchaseFlow {

    @StartableByRPC
    @InitiatingFlow
    class BikePurchaseInitiatingFlow(
        val bikeSerialNumber: String,
        val tokenIdentifierString: String,
        val fractionDigits: Int,
        val seller: Party
    ) : FlowLogic<BikeCommercializationFlowResponse>() {

        @Suspendable
        override fun call(): BikeCommercializationFlowResponse {

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val sellerSession = initiateFlow(seller)

            sellerSession.send(bikeSerialNumber)

            val bikePrice: Double =
                sellerSession.receive<Double>().unwrap { it }

            val tokenIdentifier = TokenType(tokenIdentifierString, fractionDigits)

            val amountOfBikeCoins = bikePrice of tokenIdentifier

            val txBuilder = TransactionBuilder(notary = notary)

            /*** You can use this method - work same way as 'addMoveTokens' below
            addMoveFungibleTokens(txBuilder, serviceHub, amountOfBikeCoins, seller, ourIdentity)
             */

            val inputsAndOutputs: Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> =
                DatabaseTokenSelection(serviceHub).generateMove(
                    listOf(
                        Pair(
                            sellerSession.counterparty,
                            amountOfBikeCoins
                        )
                    ), ourIdentity
                )

            addMoveTokens(txBuilder, inputsAndOutputs.first, inputsAndOutputs.second)

            val initialSignedTrnx = serviceHub.signInitialTransaction(txBuilder)

            val ftx = subFlow(CollectSignaturesFlow(initialSignedTrnx, listOf(sellerSession)))

            val stx = subFlow(FinalityFlow(ftx, listOf(sellerSession)))

            subFlow(UpdateDistributionListFlow(stx))

            return BikeCommercializationFlowResponse(
                txId = stx.id.toHexString(),
                bikeSerialNumber = bikeSerialNumber,
                buyer = ourIdentity,
                seller = seller,
                priceInCoins = bikePrice
            )
        }

    }

    @InitiatedBy(BikePurchaseInitiatingFlow::class)
    class BikePurchaseResponderFlow(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val bikeSerialNumber = counterpartySession.receive<String>().unwrap { it }

            val vaultBikeTokenQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

            val vaultPage = vaultBikeTokenQueryService.getBikeTokenBySerialNumber(
                serialNumber = bikeSerialNumber
            )

            if (vaultPage.states.isEmpty())
                throw FlowException("No states with 'serialNumber' - $bikeSerialNumber found")

            val bikeStateAndRef = vaultPage.states.single()

            val bikeStatePointer = bikeStateAndRef.state.data.toPointer<BikeTokenState>()

            val partyAndToken = PartyAndToken(counterpartySession.counterparty, bikeStatePointer)

            counterpartySession.send(bikeStateAndRef.state.data.coinPrice)

            subFlow(ReceiveFinalityFlow(counterpartySession))

            return subFlow(MoveNonFungibleTokens(partyAndToken))
        }

    }

}