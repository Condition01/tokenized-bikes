package br.com.tokenizedbikes.flows.bikecoins.models

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
open class BikeCoinMoveFlowResponse(
    txId: String,
    amount: Double,
    tokenType: TokenType,
    val oldHolderName: String,
    val newHolderName: String
): BaseBikeCoinFlowResponse(
    txId = txId,
    amount = amount,
    tokenType = tokenType
)