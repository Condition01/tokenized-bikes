package br.com.tokenizedbikes.flows.carbon.models

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class CarbonIssueFlowResponse(
    txId: String,
    bikeSerialNumber: String,
    bikeTokenLinearId: UniqueIdentifier,
    val holderName: String
): BaseBikeFlowResponse(
    txId = txId,
    bikeSerialNumber = bikeSerialNumber,
    bikeTokenLinearId = bikeTokenLinearId
)