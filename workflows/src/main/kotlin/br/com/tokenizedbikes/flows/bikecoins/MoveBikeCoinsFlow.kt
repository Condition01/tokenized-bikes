package br.com.tokenizedbikes.flows.bikecoins

import br.com.tokenizedbikes.flows.bikecoins.models.BikeCoinMoveFlowResponse
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder

object MoveBikeCoinsFlow {

    @StartableByRPC
    @InitiatingFlow
    class MoveBikeCoinInitiatingFlow(
        val amount: Double,
        val tokenIdentifierString: String,
        val fractionDigits: Int,
        val newHolder: Party
    ) : FlowLogic<BikeCoinMoveFlowResponse>() {

        @Suspendable
        override fun call(): BikeCoinMoveFlowResponse {

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val session = initiateFlow(newHolder)

            val txBuilder = TransactionBuilder(notary = notary)

            val tokenIdentifier = TokenType(tokenIdentifierString, fractionDigits)

            val amountOfBikeCoins = amount of tokenIdentifier

            addMoveFungibleTokens(txBuilder, serviceHub, amountOfBikeCoins, newHolder, ourIdentity)

            val ptx = serviceHub.signInitialTransaction(txBuilder)

            val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))

            subFlow(ObserverAwareFinalityFlow(stx, listOf(session)))

            subFlow(UpdateDistributionListFlow(stx))

            return BikeCoinMoveFlowResponse(
                txId = stx.id.toHexString(),
                amount = amount,
                tokenType = tokenIdentifier,
                oldHolderName = ourIdentity.name.organisation,
                newHolderName = newHolder.name.organisation
            )
        }

    }

    @InitiatedBy(MoveBikeCoinInitiatingFlow::class)
    class MoveBikeCoinResponderFlow(private val counterPartySession: FlowSession): FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            subFlow(ObserverAwareFinalityFlowHandler(counterPartySession))
        }

    }

}