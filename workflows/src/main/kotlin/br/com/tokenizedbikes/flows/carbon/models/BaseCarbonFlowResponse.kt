package br.com.tokenizedbikes.flows.carbon.models

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
open class adas(
    open val txId: String,
    open val bikeSerialNumber: String,
    open val bikeTokenLinearId: UniqueIdentifier
)