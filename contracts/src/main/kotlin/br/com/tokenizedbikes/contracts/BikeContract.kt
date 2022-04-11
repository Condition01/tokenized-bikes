package br.com.tokenizedbikes.contracts

import br.com.tokenizedbikes.states.BikeTokenState
import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class BikeContract : EvolvableTokenContract() {
    companion object {
        const val ID = "br.com.tokenizedbikes.contracts.BikeContract"
    }

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        requireThat {
            val newToken = tx.outputStates.single() as BikeTokenState
            "Serial Number cant be empty".using(newToken.serialNumber != "")
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
    }
}