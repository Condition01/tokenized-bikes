package br.com.tokenizedbikes.flows.biketoken.models

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
class BikeIssueFlowResponse(
    txId: String,
    bikeSerialNumber: String,
    bikeTokenLinearId: UniqueIdentifier,
    val holderName: String
): BaseBikeFlowResponse(
    txId = txId,
    bikeSerialNumber = bikeSerialNumber,
    bikeTokenLinearId = bikeTokenLinearId
)