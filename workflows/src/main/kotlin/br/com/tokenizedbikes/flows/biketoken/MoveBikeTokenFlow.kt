package br.com.tokenizedbikes.flows.biketoken

import br.com.tokenizedbikes.flows.accounts.GetAccountPubKeyAndEncapsulate
import br.com.tokenizedbikes.flows.biketoken.models.BikeMoveFlowResponse
import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder

object MoveBikeTokenFlow {

    @StartableByRPC
    class MoveBikeTokenSimpleFlow(
        private val serialNumber: String,
        private val holderAccountInfo: AccountInfo,
        private val newHolderAccountInfo: AccountInfo
    ) : FlowLogic<BikeMoveFlowResponse>() {

        @Suspendable
        override fun call(): BikeMoveFlowResponse {
            requireThat { "The holder account doens't exist" using (holderAccountInfo.host == ourIdentity) }

            val vaultBikeTokenQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

            val vaultPage = vaultBikeTokenQueryService.getBikeTokenBySerialNumber(
                serialNumber = serialNumber
            )

            if (vaultPage.states.isEmpty())
                throw FlowException("No states with 'serialNumber' - $serialNumber found")

            val bikeStateAndRef = vaultPage.states.single()

            val bikeState = bikeStateAndRef.state.data

            val bikeStatePointer = bikeState.toPointer(bikeState.javaClass)

            val newHolderParty = subFlow(GetAccountPubKeyAndEncapsulate(newHolderAccountInfo))

            val partyAndToken = PartyAndToken(newHolderParty, bikeStatePointer)

            val bikeTokenSelectionCriteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED,
                externalIds = listOf(holderAccountInfo.identifier.id)
            )

            val stx = subFlow(MoveNonFungibleTokens(partyAndToken, listOf(), bikeTokenSelectionCriteria))

            return BikeMoveFlowResponse(
                txId = stx.id.toHexString(),
                bikeSerialNumber = serialNumber,
                bikeTokenLinearId = bikeState.linearId,
                oldHolderName = ourIdentity.name.organisation,
                newHolderName = newHolderAccountInfo.name
            )
        }

    }

    @StartableByRPC
    @InitiatingFlow
    class MoveBikeTokenInitiatingFlow(
        private val serialNumber: String,
        private val holderAccountInfo: AccountInfo,
        private val newHolderAccountInfo: AccountInfo
    ) : FlowLogic<BikeMoveFlowResponse>() {

        @Suspendable
        override fun call(): BikeMoveFlowResponse {

            val vaultBikeTokenQueryService = serviceHub.cordaService(VaultBikeTokenQueryService::class.java)

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val vaultPage = vaultBikeTokenQueryService.getBikeTokenBySerialNumber(
                serialNumber = serialNumber
            )

            if (vaultPage.states.isEmpty())
                throw FlowException("No states with 'serialNumber' - $serialNumber found")

            val bikeStateAndRef = vaultPage.states.single()

            val bikeState = bikeStateAndRef.state.data

            val bikeStatePointer = bikeState.toPointer(bikeState.javaClass)

            val session = initiateFlow(newHolderAccountInfo.host)

            val txBuilder = TransactionBuilder(notary = notary)

            val newHolderParty = subFlow(GetAccountPubKeyAndEncapsulate(newHolderAccountInfo))

            val bikeTokenSelectionCriteria = QueryCriteria.VaultQueryCriteria(
                status = Vault.StateStatus.UNCONSUMED,
                externalIds = listOf(holderAccountInfo.identifier.id)
            )

            addMoveNonFungibleTokens(
                txBuilder,
                serviceHub,
                bikeStatePointer,
                newHolderParty,
                bikeTokenSelectionCriteria
            )

            val signers = txBuilder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub) + ourIdentity.owningKey

            val stx = serviceHub.signInitialTransaction(txBuilder, signers)

            subFlow(ObserverAwareFinalityFlow(stx, listOf(session)))

            subFlow(UpdateDistributionListFlow(stx))

            return BikeMoveFlowResponse(
                txId = stx.id.toHexString(),
                bikeSerialNumber = serialNumber,
                bikeTokenLinearId = bikeState.linearId,
                oldHolderName = ourIdentity.name.organisation,
                newHolderName = newHolderAccountInfo.name
            )
        }

    }

    @InitiatedBy(MoveBikeTokenInitiatingFlow::class)
    class MoveBikeTokenResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            subFlow(ObserverAwareFinalityFlowHandler(counterPartySession))
        }

    }

}