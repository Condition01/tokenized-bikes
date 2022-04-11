package br.com.tokenizedbikes.flows.bikecoins.models

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
open class BikeCoinIssueFlowResponse(
    open val txId: String,
    open val amount: Double,
    open val tokenType: TokenType
)