package br.com.tokenizedbikes.states

import br.com.tokenizedbikes.contracts.BikeContract
import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeModelDTO
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(BikeContract::class)
data class BikeState (
    val brand: String,
    val modelName: String,
    val color: BikeColor,
    val bikeImageURL: String,
    val serialNumber: Int,
    val year: Int,
    val percentOfConservation: Double,
    val holder: Party,
    val maintainer: Party,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableTokenType() {

    companion object {
        fun dtoToState(bikeModelDTO: BikeModelDTO, holder: Party, maintainer: Party): BikeState {
            return BikeState(
                brand = bikeModelDTO.brand,
                modelName =  bikeModelDTO.modelName,
                color = bikeModelDTO.color,
                bikeImageURL = bikeModelDTO.bikeImageURL,
                year = bikeModelDTO.year,
                serialNumber = bikeModelDTO.serialNumber,
                percentOfConservation = bikeModelDTO.percentOfConservation,
                holder = holder,
                maintainer = maintainer
            );
        }
    }

    override val maintainers: List<Party>
        get() = listOf(maintainer)
    override val fractionDigits: Int
        get() = 0
    }