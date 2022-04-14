package br.com.tokenizedbikes.flows.biketoken.models

import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class BikeCommercializationFlowResponse(
    txId: String,
    bikeSerialNumber: String,
    val priceInCoins: Double,
    val seller: Party,
    val buyer: Party
): BaseBikeFlowResponse(
    txId = txId,
    bikeSerialNumber = bikeSerialNumber
)