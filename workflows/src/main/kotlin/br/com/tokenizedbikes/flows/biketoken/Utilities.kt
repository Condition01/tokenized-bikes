package br.com.tokenizedbikes.flows.biketoken

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowException
import net.corda.core.identity.AbstractParty
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder

object Utilities {

    @Suspendable
    fun addMoveFungibleTokensWithFlowException(builder: TransactionBuilder, serviceHub: ServiceHub, price: Amount<TokenType>, sellerParty: AbstractParty, buyerParty: AbstractParty, criteria: QueryCriteria.VaultQueryCriteria){
        try {
            addMoveFungibleTokens(builder, serviceHub, price, sellerParty, buyerParty, criteria)
        } catch (e: Exception){
            throw FlowException(e.toString())
        }
    }

}