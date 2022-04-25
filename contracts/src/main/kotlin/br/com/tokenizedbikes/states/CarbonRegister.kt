package br.com.tokenizedbikes.states

import br.com.tokenizedbikes.contracts.CarbonReportContract
import br.com.tokenizedbikes.models.CarbonReportDTO
import br.com.tokenizedbikes.schemas.CarbonReportSchemaV1
import br.com.tokenizedbikes.schemas.PersistentBikeTokenSchemaV1
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

@BelongsToContract(CarbonReportContract::class)
data class CarbonReport (
    val maintainer: Party,
    val batchId: UniqueIdentifier,
    val description: String,
    val issued: Boolean = false,
    val issuingParty: Party? = null,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableTokenType(), QueryableState {
    companion object {
        fun dtoToState(carbonReportDTO: CarbonReportDTO, maintainer: Party): CarbonReport {
            return CarbonReport(
                batchId = carbonReportDTO.batchId,
                description = carbonReportDTO.description,
                issued = false,
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
            is CarbonReportSchemaV1 -> {
                CarbonReportSchemaV1.PersistentCarbonReport(
                    linearId = this.linearId.toString(),
                    issuingParty = this.issuingParty,
                    issued = this.issued,
                    description = this.description,
                    batchId = this.batchId.toString(),
                    maintainer = this.maintainer
                )
            }
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        TODO("Not yet implemented")
    }
}