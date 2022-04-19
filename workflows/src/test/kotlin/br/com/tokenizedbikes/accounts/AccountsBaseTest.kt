package br.com.tokenizedbikes.accounts

import br.com.tokenizedbikes.FlowTests
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.OurAccounts
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import kotlin.test.assertEquals


class AccountsBaseTest: FlowTests() {

    @Test
    fun `Create Account - Vanilla Test`() {
        val createAccountFlow = CreateAccount("bruno-acc");

        val accInfo = nodeA.runFlow(createAccountFlow).getOrThrow()
        val ourAccounts = OurAccounts()
        val aAccountsQuery = nodeA.runFlow(ourAccounts).getOrThrow()

        assertEquals(accInfo, aAccountsQuery.first())
    }

}