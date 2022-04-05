package br.com.tokenizedbikes.states

import br.com.tokenizedbikes.contracts.BikeContract
import br.com.tokenizedbikes.models.BikeColor
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(BikeContract::class)
data class BikeState (
    val brand: String,
    val modelName: String,
    val color: BikeColor,
    val year: Int,
    val percentOfConservation: Double,
    val holder: Party,
    override val linearId: UniqueIdentifier
) : EvolvableTokenType() {
    override val maintainers: List<Party>
        get() = listOf()
    override val fractionDigits: Int
        get() = 0
    }