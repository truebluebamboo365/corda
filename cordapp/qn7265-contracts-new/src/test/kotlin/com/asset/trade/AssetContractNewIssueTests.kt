package com.qn7265.trade

import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

/**
 * Practical exercise instructions for Contracts Part 2.
 * The objective here is to write some contract code that verifies a transaction to issue an [AssetStateNewNew].
 * You should be able to uncomment each test and pass it without making additional changes to your code.
 */
class AssetContractNewNewIssueTests {
    private val ledgerServices = MockServices(listOf("com.qn7265.trade"))
    private val owner  = TestIdentity(CordaX500Name("Owner", "New York", "US"))
    private val dummy = TestIdentity(CordaX500Name("Dummy", "New York", "US"))
    private val ownerParty = owner.party
    private val dummyParty = dummy.party

    private val name = "I am an asset!"
    private val amount: Long = 100

    /**
     * Task 1.
     * Recall that Commands are required to hint to the intention of the transaction as well as take a list of
     * public keys as parameters which correspond to the required signers for the transaction.
     * Commands also become more important later on when multiple actions are possible with an AssetStateNew, e.g. Trade
     * and Transfer.
     * TODO: Add an "Issue" command to the AssetContractNew and check for the existence of the command in the verify function.
     * Hint:
     * - The command should be defined inside [AssetContractNew].
     * - You can use the [requireSingleCommand] function on [tx.commands] to check for the existence and type of the specified command
     *   in the transaction.
     * - We usually encapsulate our commands around an interface inside the contract class called [Commands] which
     *   implements the [CommandData] interface. The [Issue] command itself should be defined inside the [Commands]
     *   interface as well as implement it, for example:
     *
     *     interface Commands : CommandData {
     *         class X : Commands
     *     }
     *
     * - We can check for the existence of any command that implements [AssetContractNew.Commands] by using the
     *   [requireSingleCommand] function which takes a type parameter.
     */
    @Test
    fun mustIncludeIssueCommand() {
        val asset = AssetStateNew(name, amount, ownerParty)
        ledgerServices.ledger {
            transaction {
                output(AssetContractNew.ID, asset)
                command(listOf(ownerParty.owningKey), AssetContractNew.Commands.Issue()) // Correct type.
                this.verifies()
            }
        }
    }

    /**
     * Task 2.
     * As previously observed, issue transactions should not have any input state references. Therefore we must check to
     * ensure that no input states are included in a transaction to issue an asset.
     * TODO: Write a contract constraint that ensures a transaction to issue an asset does not include any input states.
     * Hint: use a [requireThat] block with a constraint to inside the [AssetContractNew.verify] function to encapsulate your
     * constraints:
     *
     *     requireThat {
     *         "Message when constraint fails" using (boolean constraint expression)
     *     }
     *
     * Note that the unit tests often expect contract verification failure with a specific message which should be
     * defined with your contract constraints. If not then the unit test will fail!
     *
     * You can access the list of inputs via the [TransactionForContract] object which is passed into
     * [AssetContractNew.verify].
     */
    @Test
    fun issueTransactionMustHaveNoInputs() {
        val asset = AssetStateNew(name, amount, ownerParty)
        ledgerServices.ledger {
            transaction {
                input(AssetContractNew.ID, DummyState())
                command(listOf(ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, asset)
                this `fails with` "No inputs should be consumed when issuing an asset."
            }
            transaction {
                output(AssetContractNew.ID, asset)
                command(listOf(ownerParty.owningKey), AssetContractNew.Commands.Issue())
                this.verifies() // As there are no input states.
            }
        }
    }

    /**
     * Task 3.
     * Now we need to ensure that only one [AssetStateNew] is issued per transaction.
     * TODO: Write a contract constraint that ensures only one output state is created in a transaction.
     * Hint: Write an additional constraint within the existing [requireThat] block which you created in the previous
     * task.
     */
    @Test
    fun issueTransactionMustHaveOneOutput() {
        val asset = AssetStateNew(name, amount, ownerParty)
        ledgerServices.ledger {
            transaction {
                command(listOf(ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, asset) // Two outputs fails.
                output(AssetContractNew.ID, asset)
                this `fails with` "Only one output state should be created when issuing an asset."
            }
            transaction {
                command(listOf(ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, asset)  // One output passes.
                this.verifies()
            }
        }
    }

    /**
     * Task 4.
     * Now we need to consider the properties of the [AssetStateNew]. We need to ensure that an asset should always have a
     * positive value.
     * TODO: Write a contract constraint that ensures newly issued assets always have a positive value.
     * Hint: You will need a number of hints to complete this task!
     * - Use the Kotlin keyword 'val' to create a new constant which will hold a reference to the output asset state.
     * - You can use the Kotlin function [single] to either grab the single element from the list or throw an exception
     *   if there are 0 or more than one elements in the list. Note that we have already checked the outputs list has
     *   only one element in the previous task.
     * - We need to obtain a reference to the proposed asset for issuance from the transaction. The transaction can filter
     *   for inputs of a certain type if you pass the class to it.
     *   e.g.
     *          tx.outputsOfType<ContractState>()
     *   will return all outputs of class ContractState.
     *
     * - When checking the [AssetStateNew.amount] property is greater than zero, you need to check the
     *   [AssetStateNew.amount.quantity] field.
     */
    @Test
    fun cannotCreateZeroValueAssets() {
        ledgerServices.ledger {
            transaction {
                command(listOf(ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, AssetStateNew(name, 0, ownerParty)) // Zero amount fails.
                this `fails with` "A newly issued asset must have a positive amount."
            }
            transaction {
                command(listOf(ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, AssetStateNew(name, 100, ownerParty))
                this.verifies()
            }
            transaction {
                command(listOf(ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, AssetStateNew(name, 5, ownerParty))
                this.verifies()
            }
            transaction {
                command(listOf(ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, AssetStateNew(name, 10, ownerParty))
                this.verifies()
            }
        }
    }

    /**
     * Task 5.
     * The list of public keys which the commands hold should contain all of the participants defined in the [AssetStateNew].
     * The asset state you are issuing will be self issued. Further, we are inheriting from the FungibleAsset class
     * which inherits the OwnableState class which allows only one participant: the owner.
     * TODO: Add a contract constraint to check that all the required signers are [AssetStateNew] participants.
     * Hint:
     * - In Kotlin you can perform a set equality check of two sets with the == operator.
     * - We need to check that the signers for the transaction are a subset of the participants list.
     * - We don't want any additional public keys not listed in the asset's participants list.
     * - You will need a reference to the Issue command to get access to the list of signers.
     * - [requireSingleCommand] returns the single required command - you can assign the return value to a constant.
     */
    @Test
    fun ownerMustSignIssueTransaction() {
        val asset = AssetStateNew(name, amount, ownerParty)
        ledgerServices.ledger {
            transaction {
                command(dummyParty.owningKey, AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, asset)
                this `fails with` "Owner must sign asset issue transaction."
            }
            transaction {
                command(listOf(ownerParty.owningKey, dummyParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, asset)
                this `fails with` "Owner must sign asset issue transaction."
            }
            transaction {
                command(listOf(ownerParty.owningKey, ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, asset)
                this.verifies()
            }
            transaction {
                command(listOf(ownerParty.owningKey, ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, asset)
                this.verifies()
            }
        }
    }

    /**
     * Task 6.
     * In practice, we may need to enforce constraints on the nature of the asset. An example of this would be not allowing
     * assets of a certain size or greater to be issued. In our case, we will restrict the maximum asset token to 100.
     * TODO: Add a contract constraint to restrict the maximum amount of assets that can be issued to 100.
     */
    @Test
    fun cannotIssueTooManyAssets() {
        val assetOver = AssetStateNew(name, 101, ownerParty)
        val assetMax = AssetStateNew(name, 100, ownerParty)
        val assetMin = AssetStateNew(name, 1, ownerParty)
        ledgerServices.ledger {
            transaction {
                command(listOf(ownerParty.owningKey, ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, assetOver)
                this.failsWith("Cannot issue more than 100 Assets at a time.")
            }
            transaction {
                command(listOf(ownerParty.owningKey, ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, assetMax)
                this.verifies()
            }
            transaction {
                command(listOf(ownerParty.owningKey, ownerParty.owningKey), AssetContractNew.Commands.Issue())
                output(AssetContractNew.ID, assetMin)
                this.verifies()
            }
        }
    }
}
