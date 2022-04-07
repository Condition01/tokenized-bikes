package br.com.tokenizedbikes.flows

import br.com.tokenizedbikes.flows.models.BaseBikeFlowResponse
import br.com.tokenizedbikes.models.BikeUpdateModelDTO
import br.com.tokenizedbikes.service.VaultCommonQueryService
import br.com.tokenizedbikes.states.BikeTokenState
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow

object UpdateBikeTokenFlow {

    @InitiatingFlow
    class UpdateBikeTokenInitiatingFlow(
        private val linearId: UniqueIdentifier,
        private val bikeUpdateModelDTO: BikeUpdateModelDTO): FlowLogic<BaseBikeFlowResponse>() {

        @Suspendable
        override fun call(): BaseBikeFlowResponse {
            val vaultCommonQueryService = serviceHub.cordaService(VaultCommonQueryService::class.java)

            val bikeTokenStatesRef = vaultCommonQueryService.getLinearStateById<BikeTokenState>(
                linearId = linearId.toString())

            val bikeTokenStateRef = bikeTokenStatesRef.states.single()

            val bikeStateToken = bikeTokenStateRef.state.data

            val outputState = bikeStateToken.copy(
                dollarPrice = bikeUpdateModelDTO.dollarPrice,
                isNew = bikeUpdateModelDTO.isNew,
                bikeImageURL = bikeUpdateModelDTO.bikeImageURL,
                color = bikeUpdateModelDTO.color,
                percentOfConservation = bikeUpdateModelDTO.percentOfConservation,
                modelName = bikeUpdateModelDTO.modelName,
                brand = bikeUpdateModelDTO.brand,
                year = bikeUpdateModelDTO.year
            )

            val stx = subFlow(UpdateEvolvableTokenFlow(bikeTokenStateRef, outputState, listOf()))

            subFlow(UpdateDistributionListFlow(stx));

            return BaseBikeFlowResponse(
                txId =  stx.id.toHexString(),
                bikeSerialNumber = outputState.serialNumber
            )
        }

    }

    @InitiatedBy(UpdateBikeTokenInitiatingFlow::class)
    class UpdateBikeTokenResponderFlow(val counterPartySession: FlowSession): FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            subFlow(UpdateEvolvableTokenFlowHandler(counterPartySession))
        }

    }



}