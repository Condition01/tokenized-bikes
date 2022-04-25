package br.com.tokenizedbikes.states

import br.com.tokenizedbikes.contracts.BikeContract
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeModelDTO
import br.com.tokenizedbikes.schemas.PersistentBikeTokenSchemaV1
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import javax.management.Query

@BelongsToContract(BikeContract::class)
data class BikeTokenState(
    val brand: String,
    val modelName: String,
    val color: BikeColor,
    val bikeImageURL: String,
    val serialNumber: String,
    val year: Int,
    val percentOfConservation: Double,
    val dollarPrice: Double,
    val coinPrice: Double,
    val isNew: Boolean,
    val maintainer: Party,
    val issued: Boolean = false,
    val issuingParty: Party? = null,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableTokenType(), QueryableState {

    companion object {
        fun dtoToState(bikeModelDTO: BikeModelDTO, maintainer: Party): BikeTokenState {
            return BikeTokenState(
                brand = bikeModelDTO.brand,
                modelName = bikeModelDTO.modelName,
                color = bikeModelDTO.color,
                bikeImageURL = bikeModelDTO.bikeImageURL,
                year = bikeModelDTO.year,
                serialNumber = bikeModelDTO.serialNumber,
                percentOfConservation = bikeModelDTO.percentOfConservation,
                dollarPrice = bikeModelDTO.dollarPrice,
                coinPrice = bikeModelDTO.coinPrice,
                isNew = bikeModelDTO.isNew,
                maintainer = maintainer
            )
        }
    }

    override val maintainers: List<Party>
        get() = listOf(maintainer)
    override val fractionDigits: Int
        get() = 0

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PersistentBikeTokenSchemaV1 -> {
                PersistentBikeTokenSchemaV1.PersistentBikeToken(
                    linearId = this.linearId.toString(),
                    year = this.year,
                    brand = this.brand,
                    modelName = this.modelName,
                    percentOfConservation = this.percentOfConservation,
                    color = PersistentBikeTokenSchemaV1.PersistentBikeColor.fromBikeColor(this.color),
                    bikeImageURL = this.bikeImageURL,
                    isNew = this.isNew,
                    dollarPrice = this.dollarPrice,
                    coinPrice = this.coinPrice,
                    serialNumber = this.serialNumber,
                    maintainer = this.maintainer
                ).apply {
                    color!!.persistentBikeToken = this
                }
            }
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PersistentBikeTokenSchemaV1)
}