package me.callum.club_plugin.economy

import me.callum.club_plugin.economy.Blockcoin
import org.web3j.crypto.Credentials
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.FunctionEncoder
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.json.JSONObject
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt
import org.web3j.protocol.core.methods.response.EthSendTransaction
import java.math.BigInteger
import java.io.File
import java.io.FileWriter
import java.io.FileReader

class TokenizeItem(private val blockcoin: Blockcoin) {
    private val web3 = blockcoin.web3j

    // pre-generated anvil wallet private keys. don't get too excited.
    private val credentials = Credentials.create("0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80")
    private val txManager = RawTransactionManager(web3, credentials)
    private val gasProvider = DefaultGasProvider()

    private val assetFile = File("assets/assets.json") // Single JSON file for all assets

    init {
        if (!assetFile.exists()) {
            assetFile.parentFile.mkdirs() // Create directories if they don't exist
            assetFile.createNewFile() // Create the file if it doesn't exist
            val initialData = JSONObject() // Initialize with an empty JSON object
            FileWriter(assetFile).use {
                it.write(initialData.toString(4)) // Indented output
            }
        }
    }

    fun waitForReceipt(txHash: String, timeoutMs: Long = 15000): TransactionReceipt? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val receiptOpt = web3.ethGetTransactionReceipt(txHash).send().transactionReceipt
            if (receiptOpt.isPresent) {
                return receiptOpt.get()
            }
            Thread.sleep(1000)
        }
        return null
    }

    fun saveAsset(name: String, symbol: String, address: String) {
        val json = JSONObject(assetFile.readText())
        json.put("$name|$symbol", address)
        FileWriter(assetFile).use {
            it.write(json.toString(4))
        }
    }

    fun getAssetAddress(name: String, symbol: String): String? {
        val json = JSONObject(assetFile.readText())
        return json.optString("$name|$symbol", null)
    }



    // this function should be used if there is no record of the requested item's address in tokens.json
    fun checkAssetExists(name: String, symbol: String): Boolean {
        // Call getAllAssets() to get deployed asset addresses from the factory
        val getAllAssetsFunction = Function(
            "getAllAssets",
            emptyList(),
            listOf(object : TypeReference<org.web3j.abi.datatypes.DynamicArray<Address>>() {})
        )

        val encodedFunction = FunctionEncoder.encode(getAllAssetsFunction)
        val response = web3.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                credentials.address,
                blockcoin.factoryAddress,
                encodedFunction
            ),
            org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send()

        val decoded = FunctionReturnDecoder.decode(
            response.value,
            getAllAssetsFunction.outputParameters
        )

        if (decoded.isEmpty()) return false

        val assetAddresses = decoded[0].value as List<Address>

        for (assetAddress in assetAddresses) {
            try {
                // Load `name()` and `symbol()` from contract
                val nameFunc = Function(
                    "name",
                    emptyList(),
                    listOf(object : TypeReference<Utf8String>() {})
                )

                val symbolFunc = Function(
                    "symbol",
                    emptyList(),
                    listOf(object : TypeReference<Utf8String>() {})
                )

                val nameCall = web3.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        credentials.address,
                        assetAddress.toString(),
                        FunctionEncoder.encode(nameFunc)
                    ),
                    org.web3j.protocol.core.DefaultBlockParameterName.LATEST
                ).send()

                val symbolCall = web3.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        credentials.address,
                        assetAddress.toString(),
                        FunctionEncoder.encode(symbolFunc)
                    ),
                    org.web3j.protocol.core.DefaultBlockParameterName.LATEST
                ).send()

                val decodedName = FunctionReturnDecoder.decode(nameCall.value, nameFunc.outputParameters)
                val decodedSymbol = FunctionReturnDecoder.decode(symbolCall.value, symbolFunc.outputParameters)

                val existingName = decodedName.firstOrNull()?.value as? String ?: continue
                val existingSymbol = decodedSymbol.firstOrNull()?.value as? String ?: continue

                if (existingName == name && existingSymbol == symbol) {
                    println("✅ Found existing asset: $name ($symbol) at ${assetAddress.value}")
                    return true
                }

            } catch (e: Exception) {
                println("⚠️ Error checking asset at ${assetAddress.value}: ${e.message}")
            }
        }

        println("ℹ️ No matching asset found for $name ($symbol)")
        return false
    }


    // this function should be used in the event that:
    // there is no minecraft asset created by the factory that matches the required item (e.g. diamond, DIAM)
    fun createAsset(name: String, symbol: String): String? {
        // 1. Define function
        val function = Function(
            "createAsset",
            listOf(Utf8String(name), Utf8String(symbol)),
            emptyList()
        )

        // 2. Encode and send
        val encodedFunction = FunctionEncoder.encode(function)
        val txHash = txManager.sendTransaction(
            gasProvider.gasPrice,
            gasProvider.getGasLimit("createAsset"),
            blockcoin.factoryAddress,
            encodedFunction,
            BigInteger.ZERO
        ).transactionHash

        println("Transaction sent: $txHash")

        // 3. Wait for receipt
        val transactionReceipt = waitForReceipt(txHash)
        if (transactionReceipt == null) {
            println("❌ Failed to get receipt for tx: $txHash")
            return null
        }

        return null
    }

}
