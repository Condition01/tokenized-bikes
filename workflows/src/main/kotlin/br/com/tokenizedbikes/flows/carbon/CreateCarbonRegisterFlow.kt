package br.com.tokenizedbikes.flows.carbon

import br.com.tokenizedbikes.flows.carbon.models.BaseCarbonFlowResponse
import br.com.tokenizedbikes.models.CarbonRegisterDTO
import br.com.tokenizedbikes.service.CarbonQueryService
import br.com.tokenizedbikes.states.CarbonRegister
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class CreateCarbonReportFlow(
    private val carbonRegisterDTO: CarbonRegisterDTO,
    private var observers: List<Party> = emptyList()
) : FlowLogic<BaseCarbonFlowResponse>() {

    companion object {
        object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Carbon Report Creation Transaction.")
        object GENERATING_TOKEN_OBJECT : ProgressTracker.Step("Creating Carbon Report from sent DTO.")
        object CALLING_EVOLVABLE_TRANSACTION : ProgressTracker.Step("Calling Evolvable Token Creation Transaction.")

        fun tracker() = ProgressTracker(
            INITIATING_TRANSACTION,
            GENERATING_TOKEN_OBJECT,
            CALLING_EVOLVABLE_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): BaseCarbonFlowResponse {
        progressTracker.currentStep = INITIATING_TRANSACTION

        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        progressTracker.currentStep = GENERATING_TOKEN_OBJECT

        val carbonState = CarbonRegister.dtoToState(
            carbonRegisterDTO = carbonRegisterDTO,
            maintainer = ourIdentity
        )

        val carbonQueryService = serviceHub.cordaService(CarbonQueryService::class.java)

        val vaultPage = carbonQueryService.getCarbonReportByBatchId(
            batchId = carbonState.batchId
        )

        if (vaultPage.states.isNotEmpty())
            throw FlowException("A CarbonReport with same 'batchId' - ${carbonState.batchId} already exists")

        val transactionState = carbonState withNotary notary

        progressTracker.currentStep = CALLING_EVOLVABLE_TRANSACTION

        val stx = subFlow(CreateEvolvableTokens(transactionState, observers))

        return BaseCarbonFlowResponse(
            txId = stx.id.toHexString(),
            batchId = carbonState.batchId.toString(),
            carbonReportLinearId = carbonState.linearId
        )
    }

}
