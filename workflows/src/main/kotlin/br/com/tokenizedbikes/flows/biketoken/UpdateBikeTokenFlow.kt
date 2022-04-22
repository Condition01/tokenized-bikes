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
import net.corda.core.utilities.ProgressTracker

object UpdateBikeTokenFlow {

    @StartableByRPC
    @InitiatingFlow
    class UpdateBikeTokenInitiatingFlow(
        private val linearId: UniqueIdentifier,
        private val bikeUpdateModelDTO: BikeUpdateModelDTO,
        private var observers: List<Party> = emptyList()): FlowLogic<BaseBikeFlowResponse>() {

        companion object {
            object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Bike Token Update Transaction.")
            object GETTING_EVOLVABLE_TOKEN_REF: ProgressTracker.Step("Getting Evolvable Token Reference from Vault.")
            object GENERATING_TOKEN_UPDATE : ProgressTracker.Step("Generating NFT Update.")
            object CALLING_UPDATE_FLOW: ProgressTracker.Step("Calling Token Update Flow.")

            fun tracker() = ProgressTracker(
                INITIATING_TRANSACTION,
                GETTING_EVOLVABLE_TOKEN_REF,
                GENERATING_TOKEN_UPDATE,
                CALLING_UPDATE_FLOW
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): BaseBikeFlowResponse {
            progressTracker.currentStep = INITIATING_TRANSACTION

            val vaultCommonQueryService = serviceHub.cordaService(VaultCommonQueryService::class.java)

            progressTracker.currentStep = GETTING_EVOLVABLE_TOKEN_REF

            val bikeTokenStatesRef = vaultCommonQueryService.getLinearStateById<BikeTokenState>(
                linearId = linearId.toString())

            val bikeTokenStateRef = bikeTokenStatesRef.states.single()

            val bikeStateToken = bikeTokenStateRef.state.data

            progressTracker.currentStep = GENERATING_TOKEN_UPDATE

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

            progressTracker.currentStep = CALLING_UPDATE_FLOW

            val stx = subFlow(UpdateEvolvableTokenFlow(bikeTokenStateRef, outputState, emptyList(), observerSessions))

            subFlow(UpdateDistributionListFlow(stx))

            return BaseBikeFlowResponse(
                txId =  stx.id.toHexString(),
                bikeSerialNumber = outputState.serialNumber,
                bikeTokenLinearId = outputState.linearId
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