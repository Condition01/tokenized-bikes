package br.com.tokenizedbikes.flows.carbon

import br.com.tokenizedbikes.flows.accounts.GetAccountPubKeyAndEncapsulate
import br.com.tokenizedbikes.flows.carbon.models.CarbonIssueFlowResponse
import br.com.tokenizedbikes.service.CarbonQueryService
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.evolvable.addUpdateEvolvableToken
import com.r3.corda.lib.tokens.workflows.flows.issue.addIssueTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class IssueCarbonTokensFlow(
    private val carbonBatchId: UniqueIdentifier,
    private val holderAccountInfo: AccountInfo,
    private var observers: MutableList<Party> = mutableListOf(),
    private val amount: Double
) : FlowLogic<CarbonIssueFlowResponse>() {

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
    override fun call(): CarbonIssueFlowResponse {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        progressTracker.currentStep = INITIATING_TRANSACTION

        val carbonQueryService = serviceHub.cordaService(CarbonQueryService::class.java)

        val vaultPage = carbonQueryService.getCarbonReportByBatchId(
            batchId = carbonBatchId
        )

        if(vaultPage.states.isEmpty())
            throw FlowException("No states with 'carbonBatchId' - $carbonBatchId found")

        val carbonRegisterStateAndRef = vaultPage.states.single()

        val carbonRegisterData = carbonRegisterStateAndRef.state.data

        require(ourIdentity in carbonRegisterData.maintainers) {
            "This flow can only be started by existing maintainers of the EvolvableTokenType."
        }

        require(carbonRegisterData.usedTCO2e + amount <= carbonRegisterData.totalTCO2e) {
            "Cannot issue more tokens than the total TCO2e"
        }

        val updatedCarbonRegister = carbonRegisterData.copy(usedTCO2e = carbonRegisterData.usedTCO2e + amount)

        val txBuilder = TransactionBuilder(notary = notary)

        addUpdateEvolvableToken(
            txBuilder,
            carbonRegisterStateAndRef,
            updatedCarbonRegister
        )

        val tokenIdentifier = TokenType(carbonRegisterData.linearId.toString(), carbonRegisterData.fractionDigits)

        val holderParty = subFlow(GetAccountPubKeyAndEncapsulate(holderAccountInfo))

        val fungibleToken = amount of tokenIdentifier issuedBy ourIdentity heldBy holderParty

        val observersSession = mutableListOf<FlowSession>()

        for(observer in observers) {
            observersSession.add(initiateFlow(observer))
        }

        addIssueTokens(txBuilder, listOf(fungibleToken))

        val stx = serviceHub.signInitialTransaction(txBuilder)

        subFlow(ObserverAwareFinalityFlow(signedTransaction = stx, allSessions = observersSession))

        subFlow(UpdateDistributionListFlow(stx))

        return CarbonIssueFlowResponse(
            txId = stx.id.toHexString(),
            carbonReportLinearId = carbonRegisterData.linearId,
            batchId = carbonBatchId.toString(),
            holderName = holderAccountInfo.name

        )
    }

}
