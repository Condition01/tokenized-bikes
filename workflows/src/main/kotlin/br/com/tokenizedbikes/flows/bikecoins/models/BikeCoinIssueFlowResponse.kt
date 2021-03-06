package br.com.tokenizedbikes.flows.bikecoins.models

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
open class BikeCoinIssueFlowResponse(
    txId: String,
    amount: Double,
    tokenType: TokenType,
    val issuer: AbstractParty,
    val holder: AbstractParty
): BaseBikeCoinFlowResponse(
    txId = txId,
    amount = amount,
    tokenType = tokenType
)