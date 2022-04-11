package br.com.tokenizedbikes.biketoken

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.biketoken.CreateBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.IssueBikeTokenFlow
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.states.BikeTokenState
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
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

        assertThrows<Exception> {
            nodeB.runFlow(issueBikeFlow).getOrThrow()
        }
    }

}