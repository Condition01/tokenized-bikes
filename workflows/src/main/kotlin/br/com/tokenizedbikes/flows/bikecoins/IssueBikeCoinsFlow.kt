package br.com.tokenizedbikes.flows.bikecoins

import br.com.tokenizedbikes.flows.accounts.GetAccountPubKey
import br.com.tokenizedbikes.flows.bikecoins.models.BikeCoinIssueFlowResponse
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class IssueBikeCoinsFlow(
    private val amount: Double,
    private val tokenIdentifier: String,
    private val fractionDigits: Int,
    private val holderAccountInfo: AccountInfo,
    private var observers: List<Party> = emptyList()
) : FlowLogic<BikeCoinIssueFlowResponse>() {

    companion object {
        object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Bike Token Issue Transaction.")
        object CALLING_ISSUE_FLOW: ProgressTracker.Step("Calling Issue Tokens Flow.")

        fun tracker() = ProgressTracker(
            INITIATING_TRANSACTION,
            CALLING_ISSUE_FLOW
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): BikeCoinIssueFlowResponse {
        progressTracker.currentStep = INITIATING_TRANSACTION

        val tokenIdentifier = TokenType(tokenIdentifier, fractionDigits)

        val holderParty = subFlow(GetAccountPubKey(holderAccountInfo))

        val fungibleToken = amount of tokenIdentifier issuedBy ourIdentity heldBy holderParty

        progressTracker.currentStep = CALLING_ISSUE_FLOW

        val stx = subFlow(IssueTokens(listOf(fungibleToken), observers))

        return BikeCoinIssueFlowResponse(
            txId = stx.id.toHexString(),
            amount = amount,
            tokenType = tokenIdentifier,
            issuer = ourIdentity,
            holder = holderParty
        )
    }
}