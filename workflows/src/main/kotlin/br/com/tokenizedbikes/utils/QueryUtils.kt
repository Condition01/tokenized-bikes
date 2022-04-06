package br.com.tokenizedbikes.utils

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

object QueryUtils {

    inline fun <reified T : ContractState> getState(services: ServiceHub): Vault.Page<T> {
        val queryAllStatusServiceProvider = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return services.vaultService.queryBy(queryAllStatusServiceProvider)
    }

    inline fun <reified T : LinearState> getLinearStateById(services: ServiceHub, linearId: String): Vault.Page<T> {
        val queryAllStatusServiceProvider = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED,
            linearId = listOf(UniqueIdentifier.fromString(linearId)))
        return services.vaultService.queryBy(queryAllStatusServiceProvider)
    }

    inline fun <reified T : LinearState> getLinearStates(services: ServiceHub): Vault.Page<T> {
        val queryAllStatusServiceProvider = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return services.vaultService.queryBy(queryAllStatusServiceProvider)
    }

}