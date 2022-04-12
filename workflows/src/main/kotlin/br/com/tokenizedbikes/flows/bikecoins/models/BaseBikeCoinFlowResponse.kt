package br.com.tokenizedbikes.flows.bikecoins.models

import com.r3.corda.lib.tokens.contracts.types.TokenType

open class BaseBikeCoinFlowResponse (
    val txId: String,
    val amount: Double,
    val tokenType: TokenType
)