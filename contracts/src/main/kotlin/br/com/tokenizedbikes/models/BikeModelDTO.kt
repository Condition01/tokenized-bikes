package br.com.tokenizedbikes.models

data class BikeModelDTO (
    val brand: String,
    val modelName: String,
    val color: BikeColor,
    val bikeImageURL: String,
    val serialNumber: String,
    val year: Int,
    val percentOfConservation: Double
        )