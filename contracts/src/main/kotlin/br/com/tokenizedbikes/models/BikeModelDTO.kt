package br.com.tokenizedbikes.models

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class BikeModelDTO(
    val brand: String,
    val modelName: String,
    val color: BikeColor,
    val bikeImageURL: String,
    val serialNumber: String,
    val year: Int,
    val percentOfConservation: Double,
    val dollarPrice: Double,
    val coinPrice: Double,
    val isNew: Boolean
)