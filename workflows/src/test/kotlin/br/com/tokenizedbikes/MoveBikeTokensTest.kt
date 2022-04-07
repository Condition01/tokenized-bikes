package br.com.tokenizedbikes

import br.com.tokenizedbikes.flows.CreateBikeTokenFlow
import br.com.tokenizedbikes.flows.IssueBikeTokenFlow
import br.com.tokenizedbikes.flows.MoveBikeTokenFlow
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.states.BikeTokenState
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlow
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MoveBikeTokensTest: FlowTests() {

    @Test
    fun `Token Move Vanilla Test`() {
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

        val moveTokenTokenFlow = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", nodeC.info.legalIdentities.first())
        val result3 = nodeB.runFlow(moveTokenTokenFlow).getOrThrow()

        val bikeStateAndRef4 = nodeB.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result3.bikeSerialNumber }[0]

        val bikeStateAndRef5 = nodeC.services.vaultService.queryBy<BikeTokenState>().states
            .filter { it.state.data.serialNumber == result3.bikeSerialNumber }[0]

        assertEquals(bikeStateAndRef4, bikeStateAndRef5)

        val moveTokenTokenFlow2 = MoveBikeTokenFlow.MoveBikeTokenInitiatingFlow("21312AAAs", nodeC.info.legalIdentities.first())

        assertThrows<Exception> {
            nodeB.runFlow(moveTokenTokenFlow2).getOrThrow()
        }
    }

}