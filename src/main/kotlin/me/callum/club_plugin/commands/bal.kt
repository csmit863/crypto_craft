package me.callum.club_plugin.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.json.JSONObject
import me.callum.club_plugin.economy.WalletManager;

class Bal : CommandExecutor {
    private val client = HttpClient.newHttpClient()
    private val rpcUrl = "https://testnet.qutblockchain.club" // Update if your Anvil node runs on a different address

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Only players can check balances!").color(TextColor.color(255, 0, 0)))
            return true
        }

        val walletAddress = if (args.isNotEmpty()) {
            args[0] // Use provided address
        } else {
            WalletManager.getWallet(sender.uniqueId) ?: run {
                sender.sendMessage(Component.text("You don't have a wallet yet!").color(TextColor.color(255, 0, 0)))
                return true
            }
        }

        sender.sendMessage(Component.text("Querying balance for: $walletAddress").color(TextColor.color(0, 255, 0)))

        try {
            val jsonPayload = JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", 1)
                .put("method", "eth_getBalance")
                .put("params", listOf(walletAddress, "latest"))

            val request = HttpRequest.newBuilder()
                .uri(URI.create(rpcUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload.toString()))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            val jsonResponse = JSONObject(response.body())
            val balanceHex = jsonResponse.optString("result", "0x0") // Balance in Wei (hex)

            // Convert Hex to BigInteger and then to ETH
            val balanceWei = BigInteger(balanceHex.substring(2), 16)
            val balanceEth = BigDecimal(balanceWei).divide(BigDecimal("1000000000000000000")) // Convert wei to ETH

            sender.sendMessage(Component.text("Balance: $balanceEth ETH").color(TextColor.color(0, 255, 0)))
            return true

        } catch (e: Exception) {
            sender.sendMessage(Component.text("Error querying balance: ${e.message}").color(TextColor.color(255, 0, 0)))
            return true
        }
    }
}
