package br.com.tokenizedbikes.biketoken

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.biketoken.CreateBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.IssueBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.MoveBikeTokenFlow
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.service.VaultCommonQueryService
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class MoveBikeTokensTest: FlowTests() {

    @Test
    fun `Token Move - Vanilla Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")
        val accountStateNodeC = createAccount(network, nodeC, "Bob")
        val accountStateNodeCTwo = createAccount(network, nodeC, "Bob Brother")
        val accountStateNodeD = createAccount(network, nodeD, "Carlão")

        val nodeCQueryService = nodeC.services.cordaService(VaultCommonQueryService::class.java)

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

        nodeA.runFlow(issueBikeFlow).getOrThrow()

        val moveTokenTokenFlow = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", accountStateNodeB, accountStateNodeC)
        val resultMoveBike = nodeB.runFlow(moveTokenTokenFlow).getOrThrow()

        var nodeCBikeStatePointers = nodeCQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = resultMoveBike.bikeTokenLinearId.toString())

        assert(nodeCBikeStatePointers.states.isNotEmpty())

        nodeCBikeStatePointers = nodeCQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeCTwo,
            tokenIdentifier = resultMoveBike.bikeTokenLinearId.toString())

        assert(nodeCBikeStatePointers.states.isEmpty())

        val moveTokenTokenFlow2 = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", accountStateNodeB, accountStateNodeD)

        assertThrows<Exception> {
            nodeB.runFlow(moveTokenTokenFlow2).getOrThrow()
        }
    }

    @Test
    fun `Token Move - PING-PONG Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")
        val accountStateNodeC = createAccount(network, nodeC, "Bob")

        val nodeBQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)
        val nodeCQueryService = nodeC.services.cordaService(VaultCommonQueryService::class.java)

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

        nodeA.runFlow(issueBikeFlow).getOrThrow()

        val moveTokenTokenFlow = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", accountStateNodeB, accountStateNodeC)
        val resultMoveBike = nodeB.runFlow(moveTokenTokenFlow).getOrThrow()

        var nodeBBikeStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeB,
            tokenIdentifier = resultMoveBike.bikeTokenLinearId.toString())
        var nodeCBikeStatePointers = nodeCQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = resultMoveBike.bikeTokenLinearId.toString())

        assert(nodeBBikeStatePointers.states.isEmpty())
        assert(nodeCBikeStatePointers.states.isNotEmpty())

        val moveTokenTokenFlow2 = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", accountStateNodeC, accountStateNodeB)

        nodeC.runFlow(moveTokenTokenFlow2).getOrThrow()

        nodeBBikeStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeB,
            tokenIdentifier = resultMoveBike.bikeTokenLinearId.toString())
        nodeCBikeStatePointers = nodeCQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = resultMoveBike.bikeTokenLinearId.toString())

        assert(nodeBBikeStatePointers.states.isNotEmpty())
        assert(nodeCBikeStatePointers.states.isEmpty())

        val moveTokenTokenFlow3 = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", accountStateNodeB, accountStateNodeC)

        nodeB.runFlow(moveTokenTokenFlow3).getOrThrow()

        nodeBBikeStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeB,
            tokenIdentifier = resultMoveBike.bikeTokenLinearId.toString())
        nodeCBikeStatePointers = nodeCQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = resultMoveBike.bikeTokenLinearId.toString())

        assert(nodeBBikeStatePointers.states.isEmpty())
        assert(nodeCBikeStatePointers.states.isNotEmpty())
    }

    @Test
    fun `Token Move - Error Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")
        val accountStateNodeC = createAccount(network, nodeC, "Bob")
        val accountStateNodeD = createAccount(network, nodeD, "Carlao")

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
        nodeA.runFlow(issueBikeFlow).getOrThrow()

        val moveTokenTokenFlow = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", accountStateNodeD, accountStateNodeC)

        assertThrows<Exception> {
            nodeD.runFlow(moveTokenTokenFlow).getOrThrow()
        }
    }

}