package br.com.tokenizedbikes.models

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class BikeColor(
    val mainColor: BikeColorEnum,
    val otherColors: MutableSet<BikeColorEnum>,
    val isCustomColor: Boolean,
    val colorDescription: String
)