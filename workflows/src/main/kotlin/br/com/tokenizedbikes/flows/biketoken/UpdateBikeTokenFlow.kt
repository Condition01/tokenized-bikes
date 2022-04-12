package br.com.tokenizedbikes.flows.biketoken

import br.com.tokenizedbikes.flows.biketoken.models.BaseBikeFlowResponse
import br.com.tokenizedbikes.models.BikeUpdateModelDTO
import br.com.tokenizedbikes.service.VaultCommonQueryService
import br.com.tokenizedbikes.states.BikeTokenState
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party

object UpdateBikeTokenFlow {

    @StartableByRPC
    @InitiatingFlow
    class UpdateBikeTokenInitiatingFlow(
        private val linearId: UniqueIdentifier,
        private val bikeUpdateModelDTO: BikeUpdateModelDTO,
        private var observers: List<Party> = emptyList()): FlowLogic<BaseBikeFlowResponse>() {

        @Suspendable
        override fun call(): BaseBikeFlowResponse {
            val vaultCommonQueryService = serviceHub.cordaService(VaultCommonQueryService::class.java)

            val bikeTokenStatesRef = vaultCommonQueryService.getLinearStateById<BikeTokenState>(
                linearId = linearId.toString())

            val bikeTokenStateRef = bikeTokenStatesRef.states.single()

            val bikeStateToken = bikeTokenStateRef.state.data

            val outputState = bikeStateToken.copy(
                dollarPrice = bikeUpdateModelDTO.dollarPrice,
                coinPrice = bikeUpdateModelDTO.coinPrice,
                isNew = bikeUpdateModelDTO.isNew,
                bikeImageURL = bikeUpdateModelDTO.bikeImageURL,
                color = bikeUpdateModelDTO.color,
                percentOfConservation = bikeUpdateModelDTO.percentOfConservation,
                modelName = bikeUpdateModelDTO.modelName,
                brand = bikeUpdateModelDTO.brand,
                year = bikeUpdateModelDTO.year
            )

            val observerSessions: MutableList<FlowSession> = mutableListOf()

            if(observers.isNotEmpty()) {
                for(observer in observers) {
                    val session = initiateFlow(observer)
                    observerSessions.add(session)
                }
            }

            val stx = subFlow(UpdateEvolvableTokenFlow(bikeTokenStateRef, outputState, emptyList(), observerSessions))

            subFlow(UpdateDistributionListFlow(stx))

            return BaseBikeFlowResponse(
                txId =  stx.id.toHexString(),
                bikeSerialNumber = outputState.serialNumber
            )
        }

    }

    @InitiatedBy(UpdateBikeTokenInitiatingFlow::class)
    class UpdateBikeTokenResponderFlow(private val counterPartySession: FlowSession): FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            subFlow(UpdateEvolvableTokenFlowHandler(counterPartySession))
        }

    }



}