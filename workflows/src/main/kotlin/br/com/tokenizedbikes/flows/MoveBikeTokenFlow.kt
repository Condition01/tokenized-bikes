package br.com.tokenizedbikes.flows

import br.com.tokenizedbikes.flows.models.BikeMoveFlowResponse
import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokensHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import net.corda.core.flows.*
import net.corda.core.identity.Party

object MoveBikeTokenFlow {

    @StartableByRPC
    @InitiatingFlow
    class MoveBikeTokenInitiatingFlow(
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

            initiateFlow(newHolder)

            val stx = subFlow(MoveNonFungibleTokens(partyAndToken))

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
            return subFlow(MoveNonFungibleTokensHandler(counterPartySession))
        }

    }

}