package br.com.tokenizedbikes.flows

import br.com.tokenizedbikes.flows.models.BaseBikeFlowResponse
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.states.BikeTokenState
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class CreateBikeTokenFlow(
    private val bikeModelDTO: BikeModelDTO
) : FlowLogic<BaseBikeFlowResponse>() {

    companion object {
        object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Bike Token Creation Transaction.")
        object GENERATING_TOKEN_OBJECT : ProgressTracker.Step("Creating Bike Token from sent DTO.")
        object CALLING_EVOLVABLE_TRANSACTION : ProgressTracker.Step("Calling Evolvable Token Creation Transaction.")

        fun tracker() = ProgressTracker(
            INITIATING_TRANSACTION,
            GENERATING_TOKEN_OBJECT,
            CALLING_EVOLVABLE_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): BaseBikeFlowResponse {
        progressTracker.currentStep = INITIATING_TRANSACTION

        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        progressTracker.currentStep = GENERATING_TOKEN_OBJECT

        val bikeState = BikeTokenState.dtoToState(
            bikeModelDTO = bikeModelDTO,
            maintainer = ourIdentity
        )

        val transactionState = bikeState withNotary notary

        progressTracker.currentStep = CALLING_EVOLVABLE_TRANSACTION

        val stx = subFlow(CreateEvolvableTokens(transactionState))

        return BaseBikeFlowResponse(
            txId =  stx.id.toHexString(),
            bikeSerialNumber = bikeState.serialNumber
        );
    }

}
