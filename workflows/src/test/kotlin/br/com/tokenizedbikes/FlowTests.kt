package br.com.tokenizedbikes

import net.corda.core.concurrent.CordaFuture
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before


open class FlowTests {
    lateinit var network: MockNetwork
    lateinit var nodeA: StartedMockNode
    lateinit var nodeB: StartedMockNode
    lateinit var nodeC: StartedMockNode
    lateinit var nodeD: StartedMockNode

    @Before
    fun setup() {
        val myNetworkParameters = testNetworkParameters(minimumPlatformVersion = 4)

        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("br.com.tokenizedbikes.contracts"),
                    TestCordapp.findCordapp("br.com.tokenizedbikes.flows"),
                    TestCordapp.findCordapp("br.com.tokenizedbikes.schemas"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
                ),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB"))),
                networkParameters = myNetworkParameters
            )
        )
        nodeA = network.createPartyNode(null)
        nodeB = network.createPartyNode(null)
        nodeC = network.createPartyNode(null)
        nodeD = network.createPartyNode(null)
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    fun <T> StartedMockNode.runFlow(logic: FlowLogic<T>): CordaFuture<T> {
        return transaction {
            val result = startFlow(logic)
            network.runNetwork()
            result
        }
    }
}