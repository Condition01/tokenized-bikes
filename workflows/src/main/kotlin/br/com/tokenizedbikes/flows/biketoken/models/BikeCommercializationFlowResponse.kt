package br.com.tokenizedbikes.flows.biketoken.models

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class BikeCommercializationFlowResponse(
    txId: String,
    bikeSerialNumber: String,
    bikeTokenLinearId: UniqueIdentifier,
    val priceInCoins: Double,
    val seller: AccountInfo,
    val buyer: AccountInfo
): BaseBikeFlowResponse(
    txId = txId,
    bikeSerialNumber = bikeSerialNumber,
    bikeTokenLinearId = bikeTokenLinearId
)