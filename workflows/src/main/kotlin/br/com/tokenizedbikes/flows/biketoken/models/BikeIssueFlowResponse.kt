package br.com.tokenizedbikes.flows.biketoken.models

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class BikeIssueFlowResponse(
    txId: String,
    bikeSerialNumber: String,
    val holderName: String
): BaseBikeFlowResponse(
    txId = txId,
    bikeSerialNumber = bikeSerialNumber
)