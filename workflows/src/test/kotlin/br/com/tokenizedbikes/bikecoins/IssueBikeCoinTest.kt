package br.com.tokenizedbikes.bikecoins

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.bikecoins.IssueBikeCoinsFlow
import br.com.tokenizedbikes.service.VaultCommonQueryService
import net.corda.core.utilities.getOrThrow
import org.junit.Test

class IssueBikeCoinTest: FlowTests() {

    @Test
    fun `Coin Issue - Vanilla Test`() {
        val accountState = createAccount(network, nodeB, "Alice")

        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holderAccountInfo = accountState
        )

        val result = nodeA.runFlow(bikeIssueFlow).getOrThrow()

        val vaultCommonQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        val fungibles = vaultCommonQueryService.getFungiblesOfAccount(accountState, result.tokenType.tokenIdentifier)

        assert(fungibles.states.isNotEmpty())
    }

}