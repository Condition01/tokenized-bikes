package br.com.tokenizedbikes.service

import br.com.tokenizedbikes.schemas.PersistentBikeTokenSchemaV1
import br.com.tokenizedbikes.states.BikeTokenState
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
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

}