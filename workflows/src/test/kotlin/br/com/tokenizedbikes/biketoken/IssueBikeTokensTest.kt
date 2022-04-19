package br.com.tokenizedbikes.biketoken

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.biketoken.CreateBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.IssueBikeTokenFlow
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.service.VaultCommonQueryService
import br.com.tokenizedbikes.states.BikeTokenState
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class IssueBikeTokensTest: FlowTests() {

    @Test
    fun `Token Issue - Vanilla Test`() {
        val bikeColor = BikeColor(
            mainColor = BikeColorEnum.GREEN,
            colorDescription = "None",
            isCustomColor = false
        )

        val bikeDTO = BikeModelDTO(
            brand = "Cannondale",
            modelName = "Habit Carbon 1",
            percentOfConservation = 200.00,
            year = 2021,
            color = bikeColor,
            bikeImageURL = "https://bikeshopbarigui.com.br/upload/estoque/produto/principal-6206-604e687b067f4-cannondale-habit-1.jpg",
            serialNumber = "21312AAAs",
            dollarPrice = 200.00,
            coinPrice = 100.00,
            isNew = true
        )

        val bikeFlow = CreateBikeTokenFlow(bikeDTO)
        val result = nodeA.runFlow(bikeFlow).getOrThrow()

        val bikeStateAndRef = nodeA.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result.bikeSerialNumber }[0]

        assertNotNull(bikeStateAndRef)
        assertNotNull(bikeStateAndRef.state.data)
        assertEquals(bikeStateAndRef.state.data.serialNumber, bikeDTO.serialNumber)

        val accountService = nodeB.services.cordaService(KeyManagementBackedAccountService::class.java)

        val accountState = createAccount(network, accountService, "Alice")

        val issueBikeFlow = IssueBikeTokenFlow("21312AAAs", accountState.state.data)
        val result2 = nodeA.runFlow(issueBikeFlow).getOrThrow()

        val bikeStateAndRef2 = nodeB.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result2.bikeSerialNumber }[0]

        assertNotNull(bikeStateAndRef2)

        val queryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        val results = queryService.getNonFungiblesOfAccount(accountInfo = accountState.state.data,
            tokenIdentifier = bikeStateAndRef2.state.data.linearId.toString())

//        val participatingAccountCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria()
//            .withExternalIds(listOf(accountState.state.data.identifier.id))
//        val results: List<StateAndRef<NonFungibleToken>> = nodeB.services.vaultService
//            .queryBy(NonFungibleToken::class.java, participatingAccountCriteria).states

        assert(results.states.isNotEmpty())
    }

    @Test
    fun `Token Issue - Issuing with different peer - Error Test`() {
        val bikeColor = BikeColor(
            mainColor = BikeColorEnum.GREEN,
            colorDescription = "None",
            isCustomColor = false
        )

        val bikeDTO = BikeModelDTO(
            brand = "Cannondale",
            modelName = "Habit Carbon 1",
            percentOfConservation = 200.00,
            year = 2021,
            color = bikeColor,
            bikeImageURL = "https://bikeshopbarigui.com.br/upload/estoque/produto/principal-6206-604e687b067f4-cannondale-habit-1.jpg",
            serialNumber = "21312AAAs",
            dollarPrice = 200.00,
            coinPrice = 100.00,
            isNew = true
        )

        val bikeFlow = CreateBikeTokenFlow(bikeDTO)
        val result = nodeA.runFlow(bikeFlow).getOrThrow()

        val bikeStateAndRef = nodeA.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result.bikeSerialNumber }[0]

        assertNotNull(bikeStateAndRef)
        assertNotNull(bikeStateAndRef.state.data)
        assertEquals(bikeStateAndRef.state.data.serialNumber, bikeDTO.serialNumber)

        val accountService = nodeB.services.cordaService(KeyManagementBackedAccountService::class.java)

        val accountState = createAccount(network, accountService, "Alice")

        val issueBikeFlow = IssueBikeTokenFlow("21312AAAs", accountState.state.data)

        assertThrows<Exception> {
            nodeB.runFlow(issueBikeFlow).getOrThrow()
        }
    }

}