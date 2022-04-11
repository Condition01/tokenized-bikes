package br.com.tokenizedbikes.flows.biketoken

import br.com.tokenizedbikes.flows.biketoken.models.BikeMoveFlowResponse
import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder

object MoveBikeTokenFlow {

    @StartableByRPC
    class MoveBikeTokenSimpleFlow(
        val serialNumber: String,
        val newHolder: Party
    ): FlowLogic<BikeMoveFlowResponse>() {

        @Suspendable
        override fun call(): BikeMoveFlowResponse {
            val vaultBikeTokenQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

            val vaultPage = vaultBikeTokenQueryService.getBikeTokenBySerialNumber(
                serialNumber = serialNumber)

            if (vaultPage.states.isEmpty())
                throw FlowException("No states with 'serialNumber' - $serialNumber found")

            val bikeStateAndRef = vaultPage.states.single()

            val bikeState = bikeStateAndRef.state.data

            val bikeStatePointer = bikeState.toPointer(bikeState.javaClass)

            val partyAndToken = PartyAndToken(newHolder, bikeStatePointer)

            val stx = subFlow(MoveNonFungibleTokens(partyAndToken))

            return BikeMoveFlowResponse(
                txId = stx.id.toHexString(),
                bikeSerialNumber = serialNumber,
                oldHolderName = ourIdentity.name.organisation,
                newHolderName = newHolder.name.organisation
            )
        }

    }

    @StartableByRPC
    @InitiatingFlow
    class MoveBikeTokenInitiatingFlow(
        val serialNumber: String,
        val newHolder: Party
    ) : FlowLogic<BikeMoveFlowResponse>() {

        @Suspendable
        override fun call(): BikeMoveFlowResponse {

            val vaultBikeTokenQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val vaultPage = vaultBikeTokenQueryService.getBikeTokenBySerialNumber(
                serialNumber = serialNumber
            )

            if (vaultPage.states.isEmpty())
                throw FlowException("No states with 'serialNumber' - $serialNumber found")

            val bikeStateAndRef = vaultPage.states.single()

            val bikeState = bikeStateAndRef.state.data

            val bikeStatePointer = bikeState.toPointer(bikeState.javaClass)

            val session = initiateFlow(newHolder)

            val txBuilder = TransactionBuilder(notary = notary)

            addMoveNonFungibleTokens(txBuilder, serviceHub, bikeStatePointer, newHolder)

            val ptx = serviceHub.signInitialTransaction(txBuilder)

            val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))

            subFlow(ObserverAwareFinalityFlow(stx, listOf(session)))

            subFlow(UpdateDistributionListFlow(stx))

            return BikeMoveFlowResponse(
                txId = stx.id.toHexString(),
                bikeSerialNumber = serialNumber,
                oldHolderName = ourIdentity.name.organisation,
                newHolderName = newHolder.name.organisation
            )
        }

    }

    @InitiatedBy(MoveBikeTokenInitiatingFlow::class)
    class MoveBikeTokenResponderFlow(private val counterPartySession: FlowSession): FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            subFlow(ObserverAwareFinalityFlowHandler(counterPartySession))
        }

    }

}