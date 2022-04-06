package br.com.tokenizedbikes

import br.com.tokenizedbikes.flows.CreateBikeTokenFlow
import br.com.tokenizedbikes.flows.IssueBikeTokenFlow
import br.com.tokenizedbikes.flows.UpdateBikeTokenFlow
import br.com.tokenizedbikes.flows.models.BaseBikeFlowResponse
import br.com.tokenizedbikes.flows.models.BikeIssueFlowResponse
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.models.BikeUpdateModelDTO
import br.com.tokenizedbikes.states.BikeTokenState
import net.corda.core.concurrent.CordaFuture
import net.corda.core.flows.FlowLogic
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import org.junit.Ignore
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var nodeA: StartedMockNode
    private lateinit var nodeB: StartedMockNode

    @Before
    fun setup() {
        val myNetworkParameters = testNetworkParameters(minimumPlatformVersion = 4)

        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("br.com.tokenizedbikes.contracts"),
                    TestCordapp.findCordapp("br.com.tokenizedbikes.flows"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
                ),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB"))),
                networkParameters = myNetworkParameters
            )
        )
        nodeA = network.createPartyNode(null)
        nodeB = network.createPartyNode(null)
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun bikeTokensCreation() {
        val bikeColor = BikeColor(
            mainColor = BikeColorEnum.RED,
            colorDescription = "None",
            isCustomColor = false
        )

        val bikeDTO = BikeModelDTO(
            brand = "Caloi",
            modelName = "Elite Carbon Sport 2021",
            percentOfConservation = 100.00,
            year = 2021,
            color = bikeColor,
            bikeImageURL = "https://static.netshoes.com.br/produtos/bicicleta-caloi-elite-carbon-sport-2021/16/D28-0523-016/D28-0523-016_zoom1.jpg?ts=1645114872&ims=544x",
            serialNumber = "3LThOxSef1",
            dollarPrice = 100.00,
            isNew = true
        )

        val bikeFlow = CreateBikeTokenFlow(bikeDTO)
        val result = nodeA.runFlow(bikeFlow).getOrThrow()

        val bikeStateAndRef = nodeA.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result.bikeSerialNumber }[0]

        assertNotNull(bikeStateAndRef)
        assertNotNull(bikeStateAndRef.state.data)
        assertEquals(bikeStateAndRef.state.data.serialNumber, bikeDTO.serialNumber)
    }

    @Test
    fun `token Test Update` () {
        val bikeColor = BikeColor(
            mainColor = BikeColorEnum.RED,
            colorDescription = "None",
            isCustomColor = false
        )

        val bikeDTO = BikeModelDTO(
            brand = "Caloi",
            modelName = "Elite Carbon Sport 2021",
            percentOfConservation = 100.00,
            year = 2021,
            color = bikeColor,
            bikeImageURL = "https://static.netshoes.com.br/produtos/bicicleta-caloi-elite-carbon-sport-2021/16/D28-0523-016/D28-0523-016_zoom1.jpg?ts=1645114872&ims=544x",
            serialNumber = "3LThOxSef1",
            dollarPrice = 100.00,
            isNew = true
        )

        val bikeFlow = CreateBikeTokenFlow(bikeDTO)
        val result = nodeA.runFlow(bikeFlow).getOrThrow()

        val bikeStateAndRef = nodeA.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result.bikeSerialNumber }[0]

        assertNotNull(bikeStateAndRef)
        assertNotNull(bikeStateAndRef.state.data)
        assertEquals(bikeStateAndRef.state.data.serialNumber, bikeDTO.serialNumber)

        val bikeUpdateDTO = BikeUpdateModelDTO(
            brand = "Caloi",
            modelName = "Elite Carbon Sport 2021",
            percentOfConservation = 99.00,
            year = 2021,
            color = bikeColor,
            bikeImageURL = "https://static.netshoes.com.br/produtos/bicicleta-caloi-elite-carbon-sport-2021/16/D28-0523-016/D28-0523-016_zoom1.jpg?ts=1645114872&ims=544x",
            dollarPrice = 999.00,
            isNew = true
        )

        val bikeUpdateFlow = UpdateBikeTokenFlow.UpdateBikeTokenInitiatingFlow(bikeStateAndRef.state.data.linearId, bikeUpdateDTO)
        val result2 = nodeA.runFlow(bikeUpdateFlow).getOrThrow()

        val bikeStateAndRef2 = nodeA.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result2.bikeSerialNumber }[0]

        assertNotNull(bikeStateAndRef2)
        assertNotNull(bikeStateAndRef2.state.data)
        assertEquals(bikeStateAndRef2.state.data.serialNumber, bikeDTO.serialNumber)
        assertEquals(bikeStateAndRef2.state.data.percentOfConservation, 99.00)
        assertEquals(bikeStateAndRef2.state.data.dollarPrice, 999.00)
    }

    @Test
    fun `Bike token creation ERROR - same serialNumber`() {
        val bikeColor = BikeColor(
            mainColor = BikeColorEnum.RED,
            colorDescription = "None",
            isCustomColor = false
        )

        val bikeDTO = BikeModelDTO(
            brand = "Caloi",
            modelName = "Elite Carbon Sport 2021",
            percentOfConservation = 100.00,
            year = 2021,
            color = bikeColor,
            bikeImageURL = "https://static.netshoes.com.br/produtos/bicicleta-caloi-elite-carbon-sport-2021/16/D28-0523-016/D28-0523-016_zoom1.jpg?ts=1645114872&ims=544x",
            serialNumber = "3LThOxSef1",
            dollarPrice = 100.00,
            isNew = true
        )

        val bikeFlow = CreateBikeTokenFlow(bikeDTO)
        val result = nodeA.runFlow(bikeFlow).getOrThrow()

        val bikeStateAndRef = nodeA.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result.bikeSerialNumber }[0]

        assertNotNull(bikeStateAndRef)
        assertNotNull(bikeStateAndRef.state.data)
        assertEquals(bikeStateAndRef.state.data.serialNumber, bikeDTO.serialNumber)

        val bikeFlow2 = CreateBikeTokenFlow(bikeDTO)

        assertThrows<Exception> {
            nodeA.runFlow(bikeFlow2).getOrThrow()
        }
    }

    @Test
    fun bikeTokenIssuing() {
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
            isNew = true
        )

        val bikeFlow = CreateBikeTokenFlow(bikeDTO)
        val result = nodeA.runFlow(bikeFlow).getOrThrow()

        val bikeStateAndRef = nodeA.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result.bikeSerialNumber }[0]

        assertNotNull(bikeStateAndRef)
        assertNotNull(bikeStateAndRef.state.data)
        assertEquals(bikeStateAndRef.state.data.serialNumber, bikeDTO.serialNumber)

        val issueBikeFlow = IssueBikeTokenFlow("21312AAAs", nodeB.info.legalIdentities.first())
        val result2 = nodeA.runFlow(issueBikeFlow).getOrThrow()

        val bikeStateAndRef2 = nodeB.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result2.bikeSerialNumber }[0]

        assertNotNull(bikeStateAndRef2)
        assertNotNull(bikeStateAndRef2.state.data)
        assertEquals(bikeStateAndRef2.state.data.serialNumber, bikeDTO.serialNumber)
    }

    private fun <T> StartedMockNode.runFlow(logic: FlowLogic<T>): CordaFuture<T> {
        return transaction {
            val result = startFlow(logic)
            network.runNetwork()
            result
        }
    }
}