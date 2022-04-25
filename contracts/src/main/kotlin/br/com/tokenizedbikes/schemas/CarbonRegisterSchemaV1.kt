package br.com.tokenizedbikes.schemas

import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table

object CarbonReportSchemaV1 : MappedSchema(
    schemaFamily = PersistentBikeTokenSchema.javaClass,
    version = 1,
    mappedTypes = listOf(
        PersistentCarbonReport::class.java
    )
) {

    @Entity
    @CordaSerializable
    @Table(
        name = "tbl_carbon_report",
        indexes = [Index(name = "cr_batch_id_idx", columnList = "cr_batch_id")]
    )
    data class PersistentCarbonReport(
        var linearId: String? = null,
        @Column(name = "cr_maintainer")
        var maintainer: Party? = null,
        @Column(name = "cr_batch_id")
        var batchId: String? = null,
        @Column(name = "cr_description")
        var description: String? = null,
        @Column(name = "cr_issued")
        var issued: Boolean? = null,
        @Column(name = "cr_issuing_party")
        var issuingParty: Party? = null
    ) : PersistentState(), Serializable

}