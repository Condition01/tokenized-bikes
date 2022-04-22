package br.com.tokenizedbikes.biketoken

import br.com.tokenizedbikes.FlowTests
import br.com.tokenizedbikes.flows.bikecoins.IssueBikeCoinsFlow
import br.com.tokenizedbikes.flows.bikecoins.models.BikeCoinIssueFlowResponse
import br.com.tokenizedbikes.flows.biketoken.CreateBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.IssueBikeTokenFlow
import br.com.tokenizedbikes.flows.biketoken.models.BikeIssueFlowResponse
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import br.com.tokenizedbikes.models.BikeModelDTO
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.utilities.getOrThrow

open class CommercializationTests : FlowTests() {
    protected fun issueBikeToAccount(account: AccountInfo): BikeIssueFlowResponse {
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

    protected fun issueCoinsToAccount(account: AccountInfo): BikeCoinIssueFlowResponse {
        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 10000.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holderAccountInfo = account
        )

        return nodeA.runFlow(bikeIssueFlow).getOrThrow()
    }

    protected fun issueOneCoinToAccount(account: AccountInfo): BikeCoinIssueFlowResponse {
        val bikeIssueFlow = IssueBikeCoinsFlow(
            amount = 1.00,
            tokenIdentifier = "BCT",
            fractionDigits = 2,
            holderAccountInfo = account
        )

        return nodeA.runFlow(bikeIssueFlow).getOrThrow()
    }
}