package br.com.tokenizedbikes.models

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class BikeColorEnum(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}