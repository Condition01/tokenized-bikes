package br.com.tokenizedbikes

import br.com.tokenizedbikes.flows.CreateBikeTokenFlow
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.states.BikeTokenState
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreationBikeTokensTest: FlowTests() {

    @Test
    fun `Token Creation - Vanilla Test`() {
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
    fun `Token Creation - Duplicated 'serialNumber' - Error Test`() {
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


}