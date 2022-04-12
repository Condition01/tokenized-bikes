package br.com.tokenizedbikes.flows.biketoken

import br.com.tokenizedbikes.flows.biketoken.models.BikeIssueFlowResponse
import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.utilities.heldBy
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class IssueBikeTokenFlow(
    private val serialNumber: String,
    private val holder: Party,
    private var observers: MutableList<Party> = mutableListOf()
) : FlowLogic<BikeIssueFlowResponse>() {

    companion object {
        object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Bike Token Issue Transaction.")
        object UPDATING_ISSUER: ProgressTracker.Step("Updating Issuer Information Before Generating Tokens.")
        object GENERATING_NONFUNGIBLE_TOKEN : ProgressTracker.Step("Generating Non-Fungible Token.")
        object CALLING_ISSUE_FLOW: ProgressTracker.Step("Calling Issue Tokens Flow.")

        fun tracker() = ProgressTracker(
            INITIATING_TRANSACTION,
            UPDATING_ISSUER,
            GENERATING_NONFUNGIBLE_TOKEN,
            CALLING_ISSUE_FLOW
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): BikeIssueFlowResponse {

        progressTracker.currentStep = INITIATING_TRANSACTION

        val vaultBikeTokenQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

        val vaultPage = vaultBikeTokenQueryService.getBikeTokenBySerialNumber(
            serialNumber = serialNumber)

        if(vaultPage.states.isEmpty())
            throw FlowException("No states with 'serialNumber' - $serialNumber found")

        val bikeStateAndRef = vaultPage.states.single()

        val bikeTokenType = bikeStateAndRef.state.data

        if(bikeTokenType.issued)
            throw FlowException("You can't issue a flow already issued")

        val tokenUpdatedWithIssuer = bikeTokenType.copy(
            issued = true,
            issuingParty = ourIdentity
        )

        progressTracker.currentStep = UPDATING_ISSUER

        subFlow(UpdateEvolvableTokenFlow(bikeStateAndRef, tokenUpdatedWithIssuer, emptyList()))

        if(tokenUpdatedWithIssuer.maintainer != ourIdentity)
            throw FlowException("Only maintainers can Issue tokens")

        progressTracker.currentStep = GENERATING_NONFUNGIBLE_TOKEN

        val bikeTokenPointer = tokenUpdatedWithIssuer.toPointer(bikeTokenType.javaClass)

        val bikeIssuedTokenType = bikeTokenPointer issuedBy ourIdentity

        /* Default - Non Fungible Token */
        val bikeToken = bikeIssuedTokenType heldBy holder

        progressTracker.currentStep = CALLING_ISSUE_FLOW

        val stx = subFlow(IssueTokens(listOf(bikeToken), listOf(holder)))

        subFlow(UpdateDistributionListFlow(stx))

        return BikeIssueFlowResponse(
            txId = stx.id.toHexString(),
            bikeSerialNumber = serialNumber,
            holderName = holder.name.organisation
        )
    }

}