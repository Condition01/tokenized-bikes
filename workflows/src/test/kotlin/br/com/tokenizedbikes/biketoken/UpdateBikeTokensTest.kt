package br.com.tokenizedbikes.biketoken

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.biketoken.CreateBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.IssueBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.UpdateBikeTokenFlow
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.models.BikeUpdateModelDTO
import br.com.tokenizedbikes.states.BikeTokenState
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UpdateBikeTokensTest: FlowTests() {

    @Test
    fun `Token Update - Vanilla Test`() {
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

        val bikeUpdateFlow =
            UpdateBikeTokenFlow.UpdateBikeTokenInitiatingFlow(bikeStateAndRef.state.data.linearId, bikeUpdateDTO)
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
    fun `Token Update - After Issuing`() {
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

        val bikeUpdateFlow =
            UpdateBikeTokenFlow.UpdateBikeTokenInitiatingFlow(bikeStateAndRef.state.data.linearId, bikeUpdateDTO)

        assertThrows<Exception> {
            nodeB.runFlow(bikeUpdateFlow).getOrThrow()
        }
    }

}