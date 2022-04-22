package br.com.tokenizedbikes.biketoken

import br.com.tokenizedbikes.flows.biketoken.BikePurchaseFlow
import br.com.tokenizedbikes.service.VaultCommonQueryService
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PurchaseBikeTokensTest: CommercializationTests() {

    @Test
    fun `Token Purchase - Vanilla Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")
        val accountStateNodeC = createAccount(network, nodeC, "Bob")

        val nodeBQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)
        val nodeCQueryService = nodeC.services.cordaService(VaultCommonQueryService::class.java)

        val bikeIssueResponse = issueBikeToAccount(accountStateNodeB)
        val bikeCoinIssueResponse = issueCoinsToAccount(accountStateNodeC)

        var nodeCBikeStatePointers = nodeCQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = bikeIssueResponse.bikeTokenLinearId.toString())
        var nodeBBikeStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeB,
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

        val bikePurchaseFlow = BikePurchaseFlow.BikePurchaseInitiatingFlow(
            bikeSerialNumber = bikeIssueResponse.bikeSerialNumber,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            sellerAccount = accountStateNodeB,
            buyerAccount = accountStateNodeC
        )

        val resultBikePurchase = nodeC.runFlow(bikePurchaseFlow).getOrThrow()

        nodeCBikeStatePointers = nodeCQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeC,
            tokenIdentifier = resultBikePurchase.bikeTokenLinearId.toString())
        nodeBBikeStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = accountStateNodeB,
            tokenIdentifier = resultBikePurchase.bikeTokenLinearId.toString())
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
    fun `Token Purchase Same Node - Vanilla Test`() {
        val aliceAccount = createAccount(network, nodeB, "Alice")
        val bobAccount = createAccount(network, nodeB, "Bob")

        val nodeBQueryService = nodeB.services.cordaService(VaultCommonQueryService::class.java)

        val bikeIssueResponse = issueBikeToAccount(aliceAccount)
        val bikeCoinIssueResponse = issueCoinsToAccount(bobAccount)

        var bobStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = bobAccount,
            tokenIdentifier = bikeIssueResponse.bikeTokenLinearId.toString())
        var aliceStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = aliceAccount,
            tokenIdentifier = bikeIssueResponse.bikeTokenLinearId.toString())
        var bobCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = aliceAccount,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)
        var aliceCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = bobAccount,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)

        assert(aliceStatePointers.states.isNotEmpty())
        assert(bobStatePointers.states.isEmpty())
        assert(aliceCoins.states.isNotEmpty())
        assert(bobCoins.states.isEmpty())
        assertEquals(1000000, aliceCoins.states[0].state.data.amount.quantity)

        val bikePurchaseFlow = BikePurchaseFlow.BikePurchaseInitiatingFlow(
            bikeSerialNumber = bikeIssueResponse.bikeSerialNumber,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            sellerAccount = aliceAccount,
            buyerAccount = bobAccount
        )

        val resultBikePurchase = nodeB.runFlow(bikePurchaseFlow).getOrThrow()

        bobStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = bobAccount,
            tokenIdentifier = resultBikePurchase.bikeTokenLinearId.toString())
        aliceStatePointers = nodeBQueryService.getNonFungiblesOfAccount(accountInfo = aliceAccount,
            tokenIdentifier = resultBikePurchase.bikeTokenLinearId.toString())
        bobCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = aliceAccount,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)
        aliceCoins = nodeBQueryService.getFungiblesOfAccount(accountInfo = bobAccount,
            tokenIdentifier = bikeCoinIssueResponse.tokenType.tokenIdentifier)

        assert(bobStatePointers.states.isNotEmpty())
        assert(aliceStatePointers.states.isEmpty())
        assert(aliceCoins.states.isNotEmpty())
        assert(bobCoins.states.isNotEmpty())
        assertEquals(990000, aliceCoins.states[0].state.data.amount.quantity)
        assertEquals(10000, bobCoins.states[0].state.data.amount.quantity)
    }

    @Test
    fun `Token Purchase - No funds Test`() {
        val accountStateNodeB = createAccount(network, nodeB, "Alice")
        val accountStateNodeC = createAccount(network, nodeC, "Bob")

        val bikeIssueResponse = issueBikeToAccount(accountStateNodeB)
        issueOneCoinToAccount(accountStateNodeC)

        val bikePurchaseFlow = BikePurchaseFlow.BikePurchaseInitiatingFlow(
            bikeSerialNumber = bikeIssueResponse.bikeSerialNumber,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            sellerAccount = accountStateNodeB,
            buyerAccount = accountStateNodeC
        )

        assertThrows<Exception> {
            nodeC.runFlow(bikePurchaseFlow).getOrThrow()
        }
    }

    @Test
    fun `Token Purchase Same Node - No funds Test`() {
        val aliceAccount = createAccount(network, nodeB, "Alice")
        val bobAccount = createAccount(network, nodeB, "Bob")

        val bikeIssueResponse = issueBikeToAccount(aliceAccount)
        issueOneCoinToAccount(bobAccount)

        val bikePurchaseFlow = BikePurchaseFlow.BikePurchaseInitiatingFlow(
            bikeSerialNumber = bikeIssueResponse.bikeSerialNumber,
            tokenIdentifierString = "BCT",
            fractionDigits = 2,
            sellerAccount = aliceAccount,
            buyerAccount = bobAccount
        )

        assertThrows<Exception> {
            nodeC.runFlow(bikePurchaseFlow).getOrThrow()
        }
    }

}