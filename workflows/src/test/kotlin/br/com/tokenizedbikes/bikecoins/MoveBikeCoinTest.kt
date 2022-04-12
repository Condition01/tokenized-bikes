package br.com.tokenizedbikes.bikecoins

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.bikecoins.IssueBikeCoinsFlow
import br.com.tokenizedbikes.flows.bikecoins.MoveBikeCoinsFlow
import br.com.tokenizedbikes.service.VaultCommonQueryService
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
import kotlin.test.assertEquals

class MoveBikeCoinTest: FlowTests() {

    @Test
    fun `Coin Move - Vanilla Test`() {
        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holder = nodeB.info.legalIdentities[0]
        )

        val result = nodeA.runFlow(bikeIssueFlow).getOrThrow()

        val vaultCommonQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        val fungibles = vaultCommonQueryService.getFungibleTokensByIdentifier(result.tokenType.tokenIdentifier)

        assert(fungibles.states.isNotEmpty())
        assertEquals(1000000, fungibles.states[0].state.data.amount.quantity)

        val moveCoinFlow = MoveBikeCoinsFlow.MoveBikeCoinInitiatingFlow(
            amount = 5000.00,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            newHolder =  nodeC.info.legalIdentities[0]
        )

        val result2 = nodeB.runFlow(moveCoinFlow).getOrThrow()

        val vaultCommonQueryServiceNodeC = nodeC.services.cordaService(VaultCommonQueryService::class.java)

        val fungiblesNodeC = vaultCommonQueryServiceNodeC.getFungibleTokensByIdentifier(result2.tokenType.tokenIdentifier)

        assert(fungiblesNodeC.states.isNotEmpty())
        assertEquals(500000, fungiblesNodeC.states[0].state.data.amount.quantity)
    }

    @Test
    fun `Coin Move - Fraction Test`() {
        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holder = nodeB.info.legalIdentities[0]
        )

        val result = nodeA.runFlow(bikeIssueFlow).getOrThrow()

        val vaultCommonQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        val fungibles = vaultCommonQueryService.getFungibleTokensByIdentifier(result.tokenType.tokenIdentifier)

        assert(fungibles.states.isNotEmpty())
        assertEquals(1000000, fungibles.states[0].state.data.amount.quantity)

        val moveCoinFlow = MoveBikeCoinsFlow.MoveBikeCoinInitiatingFlow(
            amount = 5000.43,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            newHolder =  nodeC.info.legalIdentities[0]
        )

        val result2 = nodeB.runFlow(moveCoinFlow).getOrThrow()

        val vaultCommonQueryServiceNodeC = nodeC.services.cordaService(VaultCommonQueryService::class.java)

        val fungiblesNodeC = vaultCommonQueryServiceNodeC.getFungibleTokensByIdentifier(result2.tokenType.tokenIdentifier)

        assert(fungiblesNodeC.states.isNotEmpty())
        assertEquals(500043, fungiblesNodeC.states[0].state.data.amount.quantity)
    }

    @Test
    fun `Coin Move - No Funds ERROR Test`() {
        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holder = nodeB.info.legalIdentities[0]
        )

        val result = nodeA.runFlow(bikeIssueFlow).getOrThrow()

        val vaultCommonQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        val fungibles = vaultCommonQueryService.getFungibleTokensByIdentifier(result.tokenType.tokenIdentifier)

        assert(fungibles.states.isNotEmpty())
        assertEquals(1000000, fungibles.states[0].state.data.amount.quantity)

        val moveCoinFlow = MoveBikeCoinsFlow.MoveBikeCoinInitiatingFlow(
            amount = 15000.00,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            newHolder =  nodeC.info.legalIdentities[0]
        )

        assertThrows<Exception> {
            nodeB.runFlow(moveCoinFlow).getOrThrow()
        }

    }

}