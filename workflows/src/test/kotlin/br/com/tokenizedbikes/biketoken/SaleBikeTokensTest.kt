package br.com.tokenizedbikes.biketoken

import br.com.tokenizedbikes.flows.biketoken.BikeSaleFlow
import br.com.tokenizedbikes.service.VaultCommonQueryService
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SaleBikeTokensTest: CommercializationTests() {

    @Test
    fun `Token Sale - Vanilla Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")
        val accountStateNodeC = createAccount(network, nodeC, "Bob")

        val nodeBQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)
        val nodeCQueryService = nodeC.services.cordaService(VaultCommonQueryService::class.java)

        val bikeIssueResponse = issueBikeToAccount(accountStateNodeB)
        val bikeCoinIssueResponse = issueCoinsToAccount(accountStateNodeC)

        var nodeBBikeStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeB,
            tokenIdentifier = bikeIssueResponse.bikeTokenLinearId.toString())
        var nodeCBikeStatePointers = nodeCQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = bikeIssueResponse.bikeTokenLinearId.toString())
        var nodeBBikeCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = accountStateNodeB,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)
        var nodeCBikeCoins = nodeCQueryService.getFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)

        assert(nodeBBikeStatePointers.states.isNotEmpty())
        assert(nodeCBikeStatePointers.states.isEmpty())
        assert(nodeCBikeCoins.states.isNotEmpty())
        assert(nodeBBikeCoins.states.isEmpty())
        assertEquals(1000000, nodeCBikeCoins.states[0].state.data.amount.quantity)

        val saleBikeTokenFlow = BikeSaleFlow.BikeSaleInitiatingFlow(
            bikeSerialNumber = bikeIssueResponse.bikeSerialNumber,
            sellerAccount = accountStateNodeB,
            buyerAccount = accountStateNodeC,
            fractionDigits = 2,
            paymentTokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier
        )

        val resultBikeSale = nodeB.runFlow(saleBikeTokenFlow).getOrThrow()

        nodeCBikeStatePointers = nodeCQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = resultBikeSale.bikeTokenLinearId.toString())
        nodeBBikeStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeB,
            tokenIdentifier = resultBikeSale.bikeTokenLinearId.toString())
        nodeBBikeCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = accountStateNodeB,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)
        nodeCBikeCoins = nodeCQueryService.getFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)

        assert(nodeCBikeStatePointers.states.isNotEmpty())
        assert(nodeBBikeStatePointers.states.isEmpty())
        assert(nodeCBikeCoins.states.isNotEmpty())
        assert(nodeBBikeCoins.states.isNotEmpty())
        assertEquals(990000, nodeCBikeCoins.states[0].state.data.amount.quantity)
        assertEquals(10000, nodeBBikeCoins.states[0].state.data.amount.quantity)
    }

    @Test
    fun `Token Sale Same Node - Vanilla Test`() {
        val aliceAccount = createAccount(network, nodeB, "Alice")
        val bobAccount = createAccount(network, nodeB, "Bob")

        val nodeBQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        val bikeIssueResponse = issueBikeToAccount(aliceAccount)
        val bikeCoinIssueResponse = issueCoinsToAccount(bobAccount)

        var aliceStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = aliceAccount,
            tokenIdentifier = bikeIssueResponse.bikeTokenLinearId.toString())
        var bobStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = bobAccount,
            tokenIdentifier = bikeIssueResponse.bikeTokenLinearId.toString())
        var aliceBikeCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = aliceAccount,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)
        var bobBikeCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = bobAccount,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)

        assert(aliceStatePointers.states.isNotEmpty())
        assert(bobStatePointers.states.isEmpty())
        assert(bobBikeCoins.states.isNotEmpty())
        assert(aliceBikeCoins.states.isEmpty())
        assertEquals(1000000, bobBikeCoins.states[0].state.data.amount.quantity)

        val saleBikeTokenFlow = BikeSaleFlow.BikeSaleInitiatingFlow(
            bikeSerialNumber = bikeIssueResponse.bikeSerialNumber,
            sellerAccount = aliceAccount,
            buyerAccount = bobAccount,
            fractionDigits = 2,
            paymentTokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier
        )

        val resultBikeSale = nodeB.runFlow(saleBikeTokenFlow).getOrThrow()

        bobStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = bobAccount,
            tokenIdentifier = resultBikeSale.bikeTokenLinearId.toString())
        aliceStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = aliceAccount,
            tokenIdentifier = resultBikeSale.bikeTokenLinearId.toString())
        aliceBikeCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = aliceAccount,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)
        bobBikeCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = bobAccount,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)

        assert(bobStatePointers.states.isNotEmpty())
        assert(aliceStatePointers.states.isEmpty())
        assert(bobBikeCoins.states.isNotEmpty())
        assert(aliceBikeCoins.states.isNotEmpty())
        assertEquals(990000, bobBikeCoins.states[0].state.data.amount.quantity)
        assertEquals(10000, aliceBikeCoins.states[0].state.data.amount.quantity)
    }

    @Test
    fun `Token Sale - No funds Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")
        val accountStateNodeC = createAccount(network, nodeC, "Bob")

        val bikeIssueResponse = issueBikeToAccount(accountStateNodeB)
        issueOneCoinToAccount(accountStateNodeC)

        val saleBikeTokenFlow = BikeSaleFlow.BikeSaleInitiatingFlow(
            bikeSerialNumber = bikeIssueResponse.bikeSerialNumber,
            sellerAccount = accountStateNodeB,
            buyerAccount = accountStateNodeC,
            fractionDigits = 2,
            paymentTokenIdentifier = "BCT"
        )

        assertThrows<Exception> {
            nodeB.runFlow(saleBikeTokenFlow).getOrThrow()
        }
    }

}