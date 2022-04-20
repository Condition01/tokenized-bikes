package br.com.tokenizedbikes.bikecoins

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.bikecoins.IssueBikeCoinsFlow
import br.com.tokenizedbikes.flows.bikecoins.MoveBikeCoinsFlow
import br.com.tokenizedbikes.service.VaultCommonQueryService
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class MoveBikeCoinTest: FlowTests() {

    @Test
    fun `Coin Move - Vanilla Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")

        val accountStateNodeC = createAccount(network, nodeC, "Bob")

        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holderAccountInfo = accountStateNodeB
        )

        val result = nodeA.runFlow(bikeIssueFlow).getOrThrow()

        val vaultCommonQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        var fungiblesNodeB = vaultCommonQueryService.getFungiblesOfAccount(accountStateNodeB, result.tokenType.tokenIdentifier)

        assert(fungiblesNodeB.states.isNotEmpty())
        assertEquals(1000000, fungiblesNodeB.states[0].state.data.amount.quantity)

        val moveCoinFlow = MoveBikeCoinsFlow.MoveBikeCoinInitiatingFlow(
            amount = 5000.00,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            holderAccountInfo = accountStateNodeB,
            newHolderAccountInfo = accountStateNodeC
        )

        val result2 = nodeB.runFlow(moveCoinFlow).getOrThrow()

        val vaultCommonQueryServiceNodeC = nodeC.services.cordaService(VaultCommonQueryService::class.java)

        val fungiblesNodeC = vaultCommonQueryServiceNodeC.getFungiblesOfAccount(accountStateNodeC, result2.tokenType.tokenIdentifier)

        assert(fungiblesNodeC.states.isNotEmpty())
        assertEquals(500000, fungiblesNodeC.states[0].state.data.amount.quantity)

        fungiblesNodeB = vaultCommonQueryService.getFungiblesOfAccount(accountStateNodeB, result.tokenType.tokenIdentifier)
        assert(fungiblesNodeB.states.isNotEmpty())
        assertEquals(500000, fungiblesNodeC.states[0].state.data.amount.quantity)
    }

    @Test
    fun `Coin Move - Fraction Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")
        val accountStateNodeC = createAccount(network, nodeC, "Bob")

        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holderAccountInfo = accountStateNodeB
        )

        val result = nodeA.runFlow(bikeIssueFlow).getOrThrow()

        val vaultCommonQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        val fungibles = vaultCommonQueryService.getFungiblesOfAccount(accountStateNodeB, result.tokenType.tokenIdentifier)

        assert(fungibles.states.isNotEmpty())
        assertEquals(1000000, fungibles.states[0].state.data.amount.quantity)

        val moveCoinFlow = MoveBikeCoinsFlow.MoveBikeCoinInitiatingFlow(
            amount = 5000.43,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            holderAccountInfo = accountStateNodeB,
            newHolderAccountInfo = accountStateNodeC
        )

        val result2 = nodeB.runFlow(moveCoinFlow).getOrThrow()

        val vaultCommonQueryServiceNodeC = nodeC.services.cordaService(VaultCommonQueryService::class.java)

        val fungiblesNodeC = vaultCommonQueryServiceNodeC.getFungiblesOfAccount(accountStateNodeC, result2.tokenType.tokenIdentifier)

        assert(fungiblesNodeC.states.isNotEmpty())
        assertEquals(500043, fungiblesNodeC.states[0].state.data.amount.quantity)
    }

    @Test
    fun `Coin Move - No Funds ERROR Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")
        val accountStateNodeB2 = createAccount(network, nodeB, "AliceTwo")
        val accountStateNodeC = createAccount(network, nodeC, "Bob")

        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holderAccountInfo = accountStateNodeB
        )

        val result = nodeA.runFlow(bikeIssueFlow).getOrThrow()

        val bikeIssueFlow2 = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holderAccountInfo = accountStateNodeB2
        )

        nodeA.runFlow(bikeIssueFlow2).getOrThrow()

        val vaultCommonQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        val fungibles = vaultCommonQueryService.getFungiblesOfAccount(accountStateNodeB, result.tokenType.tokenIdentifier)

        assert(fungibles.states.isNotEmpty())
        assertEquals(1000000, fungibles.states[0].state.data.amount.quantity)

        val moveCoinFlow = MoveBikeCoinsFlow.MoveBikeCoinInitiatingFlow(
            amount = 15000.00,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            holderAccountInfo = accountStateNodeB,
            newHolderAccountInfo = accountStateNodeC
        )

        assertThrows<Exception> {
            nodeB.runFlow(moveCoinFlow).getOrThrow()
        }

    }

}