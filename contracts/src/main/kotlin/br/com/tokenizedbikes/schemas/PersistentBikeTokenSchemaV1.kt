package br.com.tokenizedbikes.models.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
object PersistentBikeTokenSchemaV1: MappedSchema(
    schemaFamily = PersistentBikeTokenSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentBikeToken::class.java)
) {

    

    data class PersistentBikeToken(
        val brand: String
    ) : PersistentState()

}