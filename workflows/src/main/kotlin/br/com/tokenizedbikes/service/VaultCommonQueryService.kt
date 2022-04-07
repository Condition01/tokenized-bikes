package br.com.tokenizedbikes.service

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.contextLogger

@CordaService
open class VaultCommonQueryService(val service: AppServiceHub) : SingletonSerializeAsToken() {

    private companion object {
        private val log = contextLogger()
    }

    inline fun <reified T : ContractState> getState(): Vault.Page<T> {
        val queryAllStatusServiceProvider = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return service.vaultService.queryBy(queryAllStatusServiceProvider)
    }

    inline fun <reified T : ContractState> getStatesWithCriteria(queryCriteria: QueryCriteria): Vault.Page<T> {
        return service.vaultService.queryBy(queryCriteria)
    }

    inline fun <reified T : LinearState> getLinearStateById(linearId: String): Vault.Page<T> {
        val queryAllStatusServiceProvider = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED,
            linearId = listOf(UniqueIdentifier.fromString(linearId)))
        return service.vaultService.queryBy(queryAllStatusServiceProvider)
    }

    inline fun <reified T : LinearState> getLinearStates(services: ServiceHub): Vault.Page<T> {
        val queryAllStatusServiceProvider = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return service.vaultService.queryBy(queryAllStatusServiceProvider)
    }

    inline fun <reified T : LinearState> getLinearStatesWithCriteria(queryCriteria: QueryCriteria): Vault.Page<T> {
        return service.vaultService.queryBy(queryCriteria)
    }

}