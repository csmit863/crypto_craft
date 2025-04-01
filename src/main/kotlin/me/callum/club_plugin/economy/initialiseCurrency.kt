package me.callum.club_plugin.economy

import org.bukkit.Bukkit
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import org.web3j.protocol.http.HttpService
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.Utf8String
import java.math.BigInteger
import java.util.concurrent.CompletableFuture

import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Convert


class Blockcoin {

    private val web3j: Web3j
    private val contractAddress: String = "0x5FbDB2315678afecb367f032d93F642f64180aa3" // Replace with actual contract
    private val decimals: Int = 18 // Change if your token has different decimal places

    init {
        val rpcUrl = "https://testnet.qutblockchain.club" // Replace with your Ethereum RPC URL
        web3j = Web3j.build(HttpService(rpcUrl)) // Initialize Web3 connection
    }

    /**
     * Retrieves the ERC-20 token balance for a given Ethereum address.
     * @param walletAddress The address to check the balance of.
     * @return Balance as a BigInteger (converted from smallest token unit).
     */
    public fun getBalance(walletAddress: String): CompletableFuture<BigInteger> {
        return try {
            val function = Function(
                "balanceOf",
                listOf(Address(walletAddress)),
                listOf(object : TypeReference<Uint256>() {})
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(walletAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).sendAsync()

            ethCall.thenApply { result ->
                val decodedValues = org.web3j.abi.FunctionReturnDecoder.decode(result.value, function.outputParameters)
                if (decodedValues.isNotEmpty()) {
                    (decodedValues[0] as Uint256).value // Return the balance
                } else {
                    BigInteger.ZERO
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CompletableFuture.completedFuture(BigInteger.ZERO) // Return zero on error
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    public fun sendTokens(fromAddress: String, toAddress: String, amount: Double, privateKey: String): CompletableFuture<Boolean> {

        return CompletableFuture.supplyAsync {
            try {
                // Convert amount to smallest unit (e.g., wei)
                val amountInWei = Convert.toWei(amount.toString(), Convert.Unit.ETHER).toBigInteger()
                Bukkit.getLogger().info(""+amountInWei);
                // Build the transfer function
                val transferFunction = Function(
                    "transfer",
                    listOf(Address(toAddress), Uint256(amountInWei)),
                    emptyList()
                )

                // Encode the function
                val encodedFunction = FunctionEncoder.encode(transferFunction)

                // Get the credentials for signing the transaction
                val credentials = Credentials.create(privateKey)
                Bukkit.getLogger().info("DEBUG: Private key loaded successfully.")

                // Create a transaction for calling the contract's transfer function
                val nonce = web3j.ethGetTransactionCount(fromAddress, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send().transactionCount
                val gasPrice = web3j.ethGasPrice().send().gasPrice
                val gasLimit = BigInteger.valueOf(60000) // Estimate this for token transfers (can vary)
                Bukkit.getLogger().info("nonce, gasprice, gaslimit initialised successfully")

                val rawtx = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    contractAddress,
                    amountInWei,
                    encodedFunction
                )

                // Sign the transaction
                val signedTransaction = TransactionEncoder.signMessage(rawtx, credentials)
                Bukkit.getLogger().info("tx signed")

                // Send the transaction to the network
                val response = web3j.ethSendRawTransaction("0x" + signedTransaction.toHexString()).sendAsync().get()
                Bukkit.getLogger().info("tx posted")

                if (response.hasError()) {
                    throw Exception("Transaction failed: ${response.error.message}")
                }

                true // Transaction successful
            } catch (e: Exception) {
                e.printStackTrace()
                false // Transaction failed
            }
        }
    }



    /**
     * Checks if the ERC-20 token contract exists by calling its `name()` method.
     * @return True if the contract exists and responds, false otherwise.
     */
    fun contractExists(): CompletableFuture<Boolean> {
        return try {
            val function = Function(
                "name",
                emptyList(),
                listOf(object : TypeReference<Utf8String>() {})
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(contractAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).sendAsync()

            ethCall.thenApply { result ->
                val decodedValues = org.web3j.abi.FunctionReturnDecoder.decode(result.value, function.outputParameters)
                if (decodedValues.isNotEmpty()) {
                    val contractName = (decodedValues[0] as Utf8String).value
                    contractName.isNotEmpty()
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CompletableFuture.completedFuture(false) // Return false on error
        }
    }
}
