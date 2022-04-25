package br.com.tokenizedbikes.models

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class CarbonReportDTO (
    val batchId: UniqueIdentifier,
    val totalTCO2e: Double,
    val description: String
)