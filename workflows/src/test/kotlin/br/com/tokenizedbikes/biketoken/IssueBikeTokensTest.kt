package br.com.tokenizedbikes.biketoken

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.biketoken.CreateBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.IssueBikeTokenFlow
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.service.VaultCommonQueryService
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows


class IssueBikeTokensTest: FlowTests() {

    @Test
    fun `Token Issue - Vanilla Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")

        val nodeBQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

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

        nodeA.runFlow(bikeFlow).getOrThrow()

        val issueBikeFlow = IssueBikeTokenFlow("21312AAAs", accountStateNodeB)

        val resultIssueBike = nodeA.runFlow(issueBikeFlow).getOrThrow()

        val nodeBFungibleStates = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeB,
            tokenIdentifier = resultIssueBike.bikeTokenLinearId.toString())

        assert(nodeBFungibleStates.states.isNotEmpty())
    }

    @Test
    fun `Token Issue - Issuing with different peer - Error Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")

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

        nodeA.runFlow(bikeFlow).getOrThrow()

        val issueBikeFlow = IssueBikeTokenFlow("21312AAAs", accountStateNodeB)

        assertThrows<Exception> {
            nodeB.runFlow(issueBikeFlow).getOrThrow()
        }
    }

}