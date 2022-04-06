package br.com.tokenizedbikes.flows

import br.com.tokenizedbikes.flows.models.BikeIssueFlowResponse
import br.com.tokenizedbikes.states.BikeTokenState
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.heldBy
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class IssueBikeTokenFlow(
    private val serialNumber: String,
    private val holder: Party
) : FlowLogic<BikeIssueFlowResponse>() {

    companion object {
        object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Bike Token Issue Transaction.")
        object GENERATING_NONFUNGIBLE_TOKEN : ProgressTracker.Step("Generating Non-Fungible Token.")
        object CALLING_ISSUE_FLOW: ProgressTracker.Step("Calling Issue Tokens Flow.")

        fun tracker() = ProgressTracker(
            INITIATING_TRANSACTION,
            GENERATING_NONFUNGIBLE_TOKEN,
            CALLING_ISSUE_FLOW
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): BikeIssueFlowResponse {

        progressTracker.currentStep = INITIATING_TRANSACTION

        val bikeStateAndRef = serviceHub.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == serialNumber }[0]

        val bikeTokenType = bikeStateAndRef.state.data

        progressTracker.currentStep = GENERATING_NONFUNGIBLE_TOKEN

        val bikeTokenPointer = bikeTokenType.toPointer(bikeTokenType.javaClass)

        val bikeIssuedTokenType = bikeTokenPointer issuedBy ourIdentity

        val bikeToken = bikeIssuedTokenType heldBy holder

        progressTracker.currentStep = CALLING_ISSUE_FLOW

        val stx = subFlow(IssueTokens(listOf(bikeToken)))

        return BikeIssueFlowResponse(
            txId = stx.id.toHexString(),
            bikeSerialNumber = serialNumber,
            holderName = holder.name.organisation
        )
    }

}