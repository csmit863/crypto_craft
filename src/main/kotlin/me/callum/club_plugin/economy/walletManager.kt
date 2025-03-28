package me.callum.club_plugin.economy

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*

object WalletManager : Listener  {
    private val playerWallets = mutableMapOf<UUID, String>() // Maps Minecraft UUID to Ethereum address
    private val balances = mutableMapOf<UUID, Double>() // ClubCoin balances

    fun hasWallet(playerUUID: UUID): Boolean {
        return playerWallets.containsKey(playerUUID)
    }

    fun createWallet(playerUUID: UUID) {
        if (!hasWallet(playerUUID)) {
            val ethAddress = generateEthereumAddress()
            playerWallets[playerUUID] = ethAddress
            balances[playerUUID] = 100.0 // Starting balance

            val player = Bukkit.getPlayer(playerUUID)

            player?.sendMessage("§aWallet created! Your address: $ethAddress")
        }
    }

    fun getWallet(playerUUID: UUID): String? {
        return playerWallets[playerUUID] // Returns Ethereum address
    }

    fun getBalance(playerUUID: UUID): Double {
        return balances.getOrDefault(playerUUID, 0.0)
    }

    fun setBalance(playerUUID: UUID, amount: Double) {
        balances[playerUUID] = amount
    }

    private fun generateEthereumAddress(): String {
        // implement actual logic to generate an EVM wallet keypair
        val randomHex = (1..40).map { "0123456789abcdef".random() }.joinToString("")

        return "0x$randomHex" // Mock Ethereum address (replace with real generation logic if needed)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        createWallet(player.uniqueId)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val killer = player.killer

        val lossAmount = getBalance(player.uniqueId) * 0.1 // 10% of coins lost

        if (killer != null) {
            // Transfer coins to the killer
            val killerBalance = getBalance(killer.uniqueId)
            setBalance(killer.uniqueId, killerBalance + lossAmount)
            player.sendMessage("§cYou lost $lossAmount ClubCoins to ${killer.name}!")
            killer.sendMessage("§aYou stole $lossAmount ClubCoins from ${player.name}!")
        } else {
            // Burn coins if death was not PvP
            setBalance(player.uniqueId, getBalance(player.uniqueId) - lossAmount)
            player.sendMessage("§cYou lost $lossAmount ClubCoins (burned).")
        }
    }
}
