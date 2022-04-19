//package br.com.tokenizedbikes.biketoken
//
//import br.com.tokenizedbikes.FlowTests
//import br.com.tokenizedbikes.flows.biketoken.CreateBikeTokenFlow
//import br.com.tokenizedbikes.flows.biketoken.IssueBikeTokenFlow
//import br.com.tokenizedbikes.flows.biketoken.MoveBikeTokenFlow
//import br.com.tokenizedbikes.models.BikeColor
//import br.com.tokenizedbikes.models.BikeColorEnum
//import br.com.tokenizedbikes.models.BikeModelDTO
//import br.com.tokenizedbikes.service.VaultBikeTokenQueryService
//import br.com.tokenizedbikes.states.BikeTokenState
//import net.corda.core.node.services.queryBy
//import net.corda.core.utilities.getOrThrow
//import org.junit.Test
//import org.junit.jupiter.api.assertThrows
//import kotlin.test.assertEquals
//import kotlin.test.assertNotNull
//import kotlin.test.assertTrue
//
//class MoveBikeTokensTest: FlowTests() {
//
//    @Test
//    fun `Token Move - Vanilla Test`() {
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
//        val result = nodeA.runFlow(bikeFlow).getOrThrow()
//
//        val bikeStateAndRef = nodeA.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result.bikeSerialNumber }[0]
//
//        assertNotNull(bikeStateAndRef)
//        assertNotNull(bikeStateAndRef.state.data)
//        assertEquals(bikeStateAndRef.state.data.serialNumber, bikeDTO.serialNumber)
//
//        val issueBikeFlow = IssueBikeTokenFlow("21312AAAs", nodeB.info.legalIdentities.first())
//        val result2 = nodeA.runFlow(issueBikeFlow).getOrThrow()
//
//        val bikeStateAndRef2 = nodeB.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result2.bikeSerialNumber }[0]
//
//        assertNotNull(bikeStateAndRef2)
//        assertNotNull(bikeStateAndRef2.state.data)
//        assertEquals(bikeStateAndRef2.state.data.serialNumber, bikeDTO.serialNumber)
//
//        val moveTokenTokenFlow = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", nodeC.info.legalIdentities.first())
//        val result3 = nodeB.runFlow(moveTokenTokenFlow).getOrThrow()
//
//        val bikeStateAndRef4 = nodeB.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result3.bikeSerialNumber }[0]
//
//        val bikeStateAndRef5 = nodeC.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result3.bikeSerialNumber }[0]
//
//        assertEquals(bikeStateAndRef4, bikeStateAndRef5)
//
//        val moveTokenTokenFlow2 = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", nodeB.info.legalIdentities.first())
//
//        assertThrows<Exception> {
//            nodeB.runFlow(moveTokenTokenFlow2).getOrThrow()
//        }
//    }
//
//    @Test
//    fun `Token Move - PING-PONG Test`() {
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
//        val result = nodeA.runFlow(bikeFlow).getOrThrow()
//
//        val bikeStateAndRef = nodeA.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result.bikeSerialNumber }[0]
//
//        assertNotNull(bikeStateAndRef)
//        assertNotNull(bikeStateAndRef.state.data)
//        assertEquals(bikeStateAndRef.state.data.serialNumber, bikeDTO.serialNumber)
//
//        val issueBikeFlow = IssueBikeTokenFlow("21312AAAs", nodeB.info.legalIdentities.first())
//        val result2 = nodeA.runFlow(issueBikeFlow).getOrThrow()
//
//        val bikeStateAndRef2 = nodeB.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result2.bikeSerialNumber }[0]
//
//        assertNotNull(bikeStateAndRef2)
//        assertNotNull(bikeStateAndRef2.state.data)
//        assertEquals(bikeStateAndRef2.state.data.serialNumber, bikeDTO.serialNumber)
//
//        val moveTokenTokenFlow = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", nodeC.info.legalIdentities.first())
//        val result3 = nodeB.runFlow(moveTokenTokenFlow).getOrThrow()
//
//        val bikeStateAndRef4 = nodeB.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result3.bikeSerialNumber }[0]
//
//        val bikeStateAndRef5 = nodeC.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result3.bikeSerialNumber }[0]
//
//        assertEquals(bikeStateAndRef4, bikeStateAndRef5)
//
//        val moveTokenTokenFlow2 = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", nodeB.info.legalIdentities.first())
//
//        nodeC.runFlow(moveTokenTokenFlow2).getOrThrow()
//
//        val moveTokenTokenFlow3 = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", nodeC.info.legalIdentities.first())
//        nodeB.runFlow(moveTokenTokenFlow3).getOrThrow()
//    }
//
//    @Test
//    fun `Token Move - Issued from Creation Observer Vanilla Test`() {
//        val bikeColor = BikeColor(
//            mainColor = BikeColorEnum.RED,
//            colorDescription = "None",
//            isCustomColor = false
//        )
//
//        val bikeDTO = BikeModelDTO(
//            brand = "Caloi",
//            modelName = "Elite Carbon Sport 2021",
//            percentOfConservation = 100.00,
//            year = 2021,
//            color = bikeColor,
//            bikeImageURL = "https://static.netshoes.com.br/produtos/bicicleta-caloi-elite-carbon-sport-2021/16/D28-0523-016/D28-0523-016_zoom1.jpg?ts=1645114872&ims=544x",
//            serialNumber = "3LThOxSef1",
//            dollarPrice = 100.00,
//            coinPrice = 100.00,
//            isNew = true
//        )
//
//        val bikeFlow = CreateBikeTokenFlow(bikeDTO, listOf(nodeB.info.legalIdentities[0]))
//        val result = nodeA.runFlow(bikeFlow).getOrThrow()
//
//        val bikeStateAndRef = nodeA.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result.bikeSerialNumber }[0]
//
//        assertNotNull(bikeStateAndRef)
//        assertNotNull(bikeStateAndRef.state.data)
//        assertEquals(bikeStateAndRef.state.data.serialNumber, bikeDTO.serialNumber)
//
//        val bikeStateAndRefsNodeB = nodeB.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result.bikeSerialNumber }
//
//        assertNotNull(bikeStateAndRefsNodeB)
//        assertNotNull(bikeStateAndRefsNodeB[0].state.data)
//        assertEquals(bikeStateAndRefsNodeB[0].state.data.serialNumber, bikeDTO.serialNumber)
//
////        MOVE OS TOKENS SEM TER FUNDOS O SUFICIENTE
////        assertThrows<java.lang.Exception> {
////            val moveTokenTokenExpectError = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("3LThOxSef1", nodeD.info.legalIdentities.first())
////            nodeB.startFlow(moveTokenTokenExpectError).getOrThrow()
////        }
//
//        val issueBikeFlow = IssueBikeTokenFlow("3LThOxSef1", nodeC.info.legalIdentities.first())
//        val result2 = nodeB.runFlow(issueBikeFlow).getOrThrow()
//
//        val bikeStateAndRef2 = nodeB.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result2.bikeSerialNumber }[0]
//
//        assertNotNull(bikeStateAndRef2)
//        assertNotNull(bikeStateAndRef2.state.data)
//        assertEquals(bikeStateAndRef2.state.data.serialNumber, bikeDTO.serialNumber)
//
//        val moveTokenTokenFlow = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("3LThOxSef1", nodeD.info.legalIdentities.first())
//        val result3 = nodeC.runFlow(moveTokenTokenFlow).getOrThrow()
//
//        val bikeStateAndRef3 = nodeD.services.vaultService.queryBy<BikeTokenState>().states
//            .filter { it.state.data.serialNumber == result3.bikeSerialNumber }[0]
//
//        assertNotNull(bikeStateAndRef3)
//        assertNotNull(bikeStateAndRef3.state.data)
//        assertEquals(bikeStateAndRef3.state.data.serialNumber, bikeDTO.serialNumber)
//
//        val nodeDVaultTokenQueryService = nodeD.services.cordaService(VaultBikeTokenQueryService::class.java)
//        val tokenPointer = bikeStateAndRef3.state.data.toPointer<BikeTokenState>()
//        val exists = nodeDVaultTokenQueryService.nonFungibleExists(tokenPointer)
//        assertTrue(exists, "O token deveria existir")
//    }
//
//}