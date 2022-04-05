package br.com.tokenizedbikes.contracts

import br.com.tokenizedbikes.states.TemplateState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class BikeContract : Contract {
    companion object {
        const val ID = "br.com.tokenizedbikes.contracts.BikeContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Issue>()
        val output = tx.outputsOfType<TemplateState>().first()
        when (command.value) {
            is Commands.Issue -> requireThat {

            }
        }
    }

    interface Commands : CommandData {
        class Issue : Commands
    }
}