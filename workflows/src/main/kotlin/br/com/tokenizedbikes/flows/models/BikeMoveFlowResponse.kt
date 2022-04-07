package br.com.tokenizedbikes.flows.models

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class BikeMoveFlowResponse(
    txId: String,
    bikeSerialNumber: String,
    val oldHolderName: String,
    val newHolderName: String
): BaseBikeFlowResponse(
    txId = txId,
    bikeSerialNumber = bikeSerialNumber
)