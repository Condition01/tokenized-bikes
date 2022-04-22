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
import net.corda.core.utilities.ProgressTracker

object MoveBikeTokenFlow {

    @StartableByRPC
    class MoveBikeTokenSimpleFlow(
        private val serialNumber: String,
        private val holderAccountInfo: AccountInfo,
        private val newHolderAccountInfo: AccountInfo
    ) : FlowLogic<BikeMoveFlowResponse>() {

        companion object {
            object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Bike Token Move Transaction.")
            object VERYFYING_BIKE_TOKEN : ProgressTracker.Step("Verifying if node has Bike Token reference.")
            object GENERATING_MOVE : ProgressTracker.Step("Calling Move Flow.")

            fun tracker() = ProgressTracker(
                INITIATING_TRANSACTION,
                VERYFYING_BIKE_TOKEN,
                GENERATING_MOVE
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): BikeMoveFlowResponse {
            progressTracker.currentStep = INITIATING_TRANSACTION

            requireThat { "Holder account host should match the host node" using (holderAccountInfo.host == ourIdentity) }

            progressTracker.currentStep = VERYFYING_BIKE_TOKEN

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

            progressTracker.currentStep = GENERATING_MOVE

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

        companion object {
            object INITIATING_TRANSACTION : ProgressTracker.Step("Initiating Bike Token Move Transaction.")
            object VERYFYING_BIKE_TOKEN : ProgressTracker.Step("Verifying if node has Bike Token reference.")
            object GENERATING_MOVE : ProgressTracker.Step("Calling Move Flow.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with well-know public keys.")
            object FINISHING_TRANSACTION : ProgressTracker.Step("Calling Finilaty Flow and finishing the transaction.")

            fun tracker() = ProgressTracker(
                INITIATING_TRANSACTION,
                VERYFYING_BIKE_TOKEN,
                GENERATING_MOVE
            )
        }

        override val progressTracker = MoveBikeTokenSimpleFlow.tracker()

        @Suspendable
        override fun call(): BikeMoveFlowResponse {
            progressTracker.currentStep = INITIATING_TRANSACTION

            requireThat { "Holder account host should match the host node" using (holderAccountInfo.host == ourIdentity) }

            progressTracker.currentStep = MoveBikeTokenSimpleFlow.Companion.VERYFYING_BIKE_TOKEN

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

            progressTracker.currentStep = GENERATING_MOVE

            addMoveNonFungibleTokens(
                txBuilder,
                serviceHub,
                bikeStatePointer,
                newHolderParty,
                bikeTokenSelectionCriteria
            )

            progressTracker.currentStep = SIGNING_TRANSACTION

            val signers = txBuilder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub) + ourIdentity.owningKey

            val stx = serviceHub.signInitialTransaction(txBuilder, signers)

            progressTracker.currentStep = FINISHING_TRANSACTION

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