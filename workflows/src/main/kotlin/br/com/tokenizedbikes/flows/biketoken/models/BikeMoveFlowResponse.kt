package br.com.tokenizedbikes.flows.biketoken.models

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class BikeMoveFlowResponse(
    txId: String,
    bikeSerialNumber: String,
    bikeTokenLinearId: UniqueIdentifier,
    val oldHolderName: String,
    val newHolderName: String
): BaseBikeFlowResponse(
    txId = txId,
    bikeSerialNumber = bikeSerialNumber,
    bikeTokenLinearId = bikeTokenLinearId
)