package com.qn7265.trade

import com.qn7265.issue.AssetState
import com.qn7265.issue.AssetContract
import com.twig.state.Asset
import com.twig.state.PrideState
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

/**
 * This is where you'll add the contract code which defines how the [AssetStateNew] behaves. Looks at the unit tests in
 * [AssetContractNewTests] for instructions on how to complete the [AssetContractNew] class.
 * You can copy your old verifyIssue function. Make sure it is using the new state type though!
 */
class AssetContractNew : UpgradedContractWithLegacyConstraint<AssetState, AssetStateNew> {
    // This is used to identify our contract when building a transaction
    companion object {
        val ID = "com.qn7265.trade.AssetContractNew"
    }

    override val legacyContract = "com.qn7265.issue.AssetContract"
    override val legacyContractConstraint: AttachmentConstraint
                get() = AlwaysAcceptAttachmentConstraint

    override fun upgrade(state: AssetState) = AssetStateNew(state.amount, state.owner)

    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.

        when (tx.commands.select(Commands::class.java).requireSingleCommand<Commands>().value) {
            is Commands.Issue -> verifyIssue(tx)
            is Commands.Trade -> verifyTrade(tx)
        }
    }

    private fun verifyIssue(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Issue>()
        requireThat {
                   "No inputs should be consumed when issuing an asset." using (tx.inputs.isEmpty())
                   "Only one output state should be created when issuing an asset."  using (tx.outputs.size==1)
                   val out = tx.outputsOfType<AssetStateNew>().single()
                   "A newly issued asset must have a positive amount." using (out.amount.quantity > 0)
                   "Owner must sign asset issue transaction." using(command.signers.toSet() == out.participants.map { it.owningKey }.toSet())
                   "Cannot issue more than 100 Assets at a time." using (out.amount.quantity <= 100)
                }
    }

    private fun verifyTrade(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Trade>()
        requireThat {
            val inputAsset = tx.inputsOfType<AssetStateNew>()
            val devonKey = inputAsset.first().owner.owningKey
            
            val outputPride = tx.outputsOfType<PrideState>()
            val devonPride = outputPride.filter{ it.owner.owningKey == devonKey }
            
             "There must be output Prides paid to the recipient." using (devonPride.isNotEmpty())
        
            // task 3 test
            val inputPride = tx.inputsOfType<PrideState>()
            val devonPrideKey = inputPride.first().owner.owningKey
            
            val outputAsset = tx.outputsOfType<AssetStateNew>()
            val devonAssetKey = outputAsset.filter{ it.owner.owningKey == devonPrideKey }

             "There must be output Assets paid to the recipient." using (devonAssetKey.isNotEmpty())
            
            // task 4 test - sum all amount using map!

            val inputAmount = inputAsset.map{ it.amount.quantity }.sum()
            val outputAmount = outputAsset.map{ it.amount.quantity }.sum()

             "Total number of input Assets must equal total number of output Assets." using (inputAmount == outputAmount)

            // task 5 test - Total number of input Prides must equal total number of output Prides.
            val totalInputPride = inputPride.map{ it.amount.quantity }.sum()
            val totalOutputPride = outputPride.map{ it.amount.quantity }.sum()

             "Total number of input Prides must equal total number of output Prides." using (totalInputPride == totalOutputPride)
            
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Trade: Commands
    }
}

/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [AssetStateNewTests] for
 * instructions on how to complete the [AssetStateNew] class.
 * You can copy your old AssetState. Make sure it is using the [AssetStateNew] though!
 */
data class AssetStateNew(override val amount: Amount<Issued<Asset>>, override val owner: AbstractParty) : FungibleAsset<Asset> {
    override val participants: List<AbstractParty> = listOf(owner)
    override fun withNewOwner(newOwner: AbstractParty): CommandAndState = CommandAndState(AssetContractNew.Commands.Trade(), copy(owner = newOwner))
    override fun withNewOwnerAndAmount(newAmount: Amount<Issued<Asset>>, newOwner: AbstractParty): FungibleAsset<Asset> = copy(amount = newAmount, owner = newOwner)
    override val exitKeys: Collection<PublicKey> = listOf<PublicKey>(owner.owningKey)
    
    constructor(name: String, amount: Long,  owner: AbstractParty):
            this(Amount<Issued<Asset>>(amount, Issued<Asset>(owner.ref(0), Asset(name))), owner)
}
