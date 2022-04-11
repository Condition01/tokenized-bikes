package br.com.tokenizedbikes.flows.biketoken.models

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
open class BaseBikeFlowResponse(
    open val txId: String,
    open val bikeSerialNumber: String
)