package br.com.tokenizedbikes.models

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class BikeColor(
    val mainColor: BikeColorEnum,
    val otherColors: MutableSet<BikeColorEnum> = mutableSetOf(),
    val isCustomColor: Boolean,
    val colorDescription: String
)