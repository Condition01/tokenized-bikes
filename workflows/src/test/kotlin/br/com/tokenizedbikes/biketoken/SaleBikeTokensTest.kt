//package br.com.tokenizedbikes.biketoken
//
//import br.com.tokenizedbikes.FlowTests
//import br.com.tokenizedbikes.flows.bikecoins.IssueBikeCoinsFlow
//import br.com.tokenizedbikes.flows.biketoken.BikeSaleFlow
//import br.com.tokenizedbikes.flows.biketoken.CreateBikeTokenFlow
//import br.com.tokenizedbikes.flows.biketoken.IssueBikeTokenFlow
//import br.com.tokenizedbikes.models.BikeColor
//import br.com.tokenizedbikes.models.BikeColorEnum
//import br.com.tokenizedbikes.models.BikeModelDTO
//import br.com.tokenizedbikes.service.VaultCommonQueryService
//import br.com.tokenizedbikes.states.BikeTokenState
//import net.corda.core.node.services.queryBy
//import net.corda.core.utilities.getOrThrow
//import net.corda.testing.node.StartedMockNode
//import org.junit.Test
//import org.junit.jupiter.api.assertThrows
//import java.lang.Exception
//
//class SaleBikeTokensTest: FlowTests() {
//
//    private fun issueBikeTokenForNode(node: StartedMockNode): String {
//        val bikeColor = BikeColor(
//            mainColor = BikeColorEnum.GREEN,
//            colorDescription = "None",
//            isCustomColor = false
//        )
//
//        val bikeDTO = BikeModelDTO(
//            brand = "Cannondale",
//            modelName = "Habit Carbon 1",
//            percentOfConservation = 200.00,
//            year = 2021,
//            color = bikeColor,
//            bikeImageURL = "https://bikeshopbarigui.com.br/upload/estoque/produto/principal-6206-604e687b067f4-cannondale-habit-1.jpg",
//            serialNumber = "21312AAAs",
//            dollarPrice = 200.00,
//            coinPrice = 100.00,
//            isNew = true
//        )
//
//        val bikeFlow = CreateBikeTokenFlow(bikeDTO)
//        node.runFlow(bikeFlow).getOrThrow()
//
//        val issueBikeFlow = IssueBikeTokenFlow("21312AAAs", node.info.legalIdentities.first())
//        node.runFlow(issueBikeFlow).getOrThrow()
//
//        return bikeDTO.serialNumber
//    }
//
//    private fun issueCoinsForNode(node: StartedMockNode) {
//        val bikeIssueFlow = IssueBikeCoinsFlow(
//            amount = 10000.00,
//            tokenIdentifier = "BCT",
//            fractionDigits = 2,
//            holder = node.info.legalIdentities[0]
//        )
//
//        nodeA.runFlow(bikeIssueFlow).getOrThrow()
//    }
//
//    private fun issueOneCoinForNode(node: StartedMockNode) {
//        val bikeIssueFlow = IssueBikeCoinsFlow(
//            amount = 1.00,
//            tokenIdentifier = "BCT",
//            fractionDigits = 2,
//            holder = node.info.legalIdentities[0]
//        )
//
//        nodeA.runFlow(bikeIssueFlow).getOrThrow()
//    }
//
//    @Test
//    fun `Token Sale - Vanilla Test`() { val serialNumber = issueBikeTokenForNode(nodeB)
//        issueCoinsForNode(nodeC)
//
//        val bikeStateAndRef = nodeC.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == serialNumber }
//
//        assert(bikeStateAndRef.isEmpty())
//
//        val saleBikeTokenFlow = BikeSaleFlow.BikeSaleInitiatingFlow(
//            bikeSerialNumber = serialNumber,
//            buyer = nodeC.info.legalIdentities[0],
//            fractionDigits = 2,
//            paymentTokenIdentifier = "BCT"
//        )
//
//        val result = nodeB.runFlow(saleBikeTokenFlow).getOrThrow()
//
//        val bikeStateAndRefAfterUpdate = nodeC.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result.bikeSerialNumber }
//
//        assert(bikeStateAndRefAfterUpdate.isNotEmpty())
//
//        val vaultCommonQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)
//
//        val nodeBFungibles = vaultCommonQueryService.getFungibleTokensByIdentifier("BCT")
//
//        assert(nodeBFungibles.states.isNotEmpty())
//
//        val vaultCommonQueryServiceNodeC = nodeC.services.cordaService(VaultCommonQueryService::class.java)
//
//        val nodeCFungibles = vaultCommonQueryServiceNodeC.getFungibleTokensByIdentifier("BCT")
//
//        assert(nodeCFungibles.states.isNotEmpty())
//    }
//
//    @Test
//    fun `Token Sale - No funds Test`() {
//        val serialNumber = issueBikeTokenForNode(nodeB)
//        issueOneCoinForNode(nodeC)
//
//        val bikeStateAndRef = nodeC.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == serialNumber }
//
//        assert(bikeStateAndRef.isEmpty())
//
//        val saleBikeTokenFlow = BikeSaleFlow.BikeSaleInitiatingFlow(
//            bikeSerialNumber = serialNumber,
//            buyer = nodeC.info.legalIdentities[0],
//            fractionDigits = 2,
//            paymentTokenIdentifier = "BCT"
//        )
//
//        assertThrows<Exception> {
//            nodeB.runFlow(saleBikeTokenFlow).getOrThrow()
//        }
//    }
//
//}