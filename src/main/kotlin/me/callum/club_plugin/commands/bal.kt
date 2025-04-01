package me.callum.club_plugin.commands

import me.callum.club_plugin.economy.WalletManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Bal : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Only players can check balances!").color(TextColor.color(255, 0, 0)))
            return true
        }

        val target: Player = if (args.isNotEmpty()) {
            sender.server.getPlayer(args[0]) ?: run {
                sender.sendMessage(Component.text("Player not found!").color(TextColor.color(255, 0, 0)))
                return true
            }
        } else sender

        val wallet = WalletManager.getWallet(target.uniqueId)
        if (wallet == null) {
            sender.sendMessage(Component.text("${target.name} does not have a wallet!").color(TextColor.color(255, 0, 0)))
            return true
        }

        sender.sendMessage(Component.text("Wallet Address: $wallet").color(TextColor.color(0, 255, 255)))

        // Start fetching the balance asynchronously
        WalletManager.getBalance(target.uniqueId).thenAccept { balance ->
            sender.sendMessage(Component.text("${target.name}'s Balance: ${balance} Blockcoins").color(TextColor.color(0, 255, 0)))
        }.exceptionally { e ->
            sender.sendMessage(Component.text("Error fetching balance: ${e.message}").color(TextColor.color(255, 0, 0)))
            null
        }

        return true
    }


}
