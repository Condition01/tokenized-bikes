package br.com.tokenizedbikes.service

import br.com.tokenizedbikes.schemas.PersistentBikeTokenSchemaV1
import br.com.tokenizedbikes.states.BikeTokenState
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentNonFungibleToken
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.workflows.utilities.heldTokenCriteria
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder

@CordaService
class VaultBikeTokenQueryService(service: AppServiceHub) : VaultCommonQueryService(service = service) {

    fun getBikeTokenBySerialNumber(serialNumber: String): Vault.Page<BikeTokenState> {
        val serialNumberCriteria = QueryCriteria.VaultCustomQueryCriteria(builder {
            PersistentBikeTokenSchemaV1.PersistentBikeToken::serialNumber.equal(serialNumber)
        })
        val criteria= QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED).and(serialNumberCriteria)

        return this.getStatesWithCriteria(criteria)
    }

    fun getFungibles(tokenIdentifier: String): Vault.Page<FungibleToken> {
        val identifierCriteria = builder {
            PersistentFungibleToken::tokenIdentifier.equal(tokenIdentifier)
        }
        val tokenIdentifierCriteria = QueryCriteria.VaultCustomQueryCriteria(identifierCriteria)

        return service.vaultService.queryBy(tokenIdentifierCriteria)
    }

    fun nonFungibleExists(tokenIdentifier: String): Boolean {
        val tokenIdentifier = builder {
            PersistentNonFungibleToken::tokenIdentifier.equal(tokenIdentifier)
        }
        val tokenIdentifierCriteria = QueryCriteria.VaultCustomQueryCriteria(tokenIdentifier)

        return service.vaultService.queryBy<NonFungibleToken>(tokenIdentifierCriteria).states.size == 1
    }

    fun <T: EvolvableTokenType>nonFungibleExists(tokenPointer: TokenPointer<T>): Boolean {
        val query = heldTokenCriteria(tokenPointer)
        val criteria = heldTokenCriteria(tokenPointer).and(query)

        return service.vaultService.queryBy<NonFungibleToken>(criteria).states.size == 1
    }

}