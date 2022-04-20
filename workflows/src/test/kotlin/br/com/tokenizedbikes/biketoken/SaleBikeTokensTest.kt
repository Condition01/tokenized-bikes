package br.com.tokenizedbikes.biketoken

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.bikecoins.IssueBikeCoinsFlow
import br.com.tokenizedbikes.flows.bikecoins.models.BikeCoinIssueFlowResponse
import br.com.tokenizedbikes.flows.biketoken.BikeSaleFlow
import br.com.tokenizedbikes.flows.biketoken.CreateBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.IssueBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.models.BikeIssueFlowResponse
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.service.VaultCommonQueryService
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SaleBikeTokensTest: FlowTests() {

    private fun issueBikeToAccount(account: AccountInfo): BikeIssueFlowResponse {
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
            coinPrice = 100.00,
            isNew = true
        )

        val bikeFlow = CreateBikeTokenFlow(bikeDTO)
        nodeA.runFlow(bikeFlow).getOrThrow()

        val issueBikeFlow = IssueBikeTokenFlow("21312AAAs", account)
        return nodeA.runFlow(issueBikeFlow).getOrThrow()
    }

    private fun issueCoinsToAccount(account: AccountInfo): BikeCoinIssueFlowResponse {
        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holderAccountInfo = account
        )

        return nodeA.runFlow(bikeIssueFlow).getOrThrow()
    }

    private fun issueOneCoinToAccount(account: AccountInfo): BikeCoinIssueFlowResponse {
        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 1.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holderAccountInfo = account
        )

        return nodeA.runFlow(bikeIssueFlow).getOrThrow()
    }

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