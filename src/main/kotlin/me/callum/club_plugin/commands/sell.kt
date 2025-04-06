package me.callum.club_plugin.commands

import me.callum.club_plugin.economy.TokenizeItem
import me.callum.club_plugin.economy.WalletManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.math.BigInteger


/**
 * To sell items, some contracts have to exist:
 * 1. The token contracts for Blockcoin and the specified item [x]
 * 2. The pair factory contract []
 * 3. The pair contract for Blockcoin and the tokenized item []
 * If these contracts do not exist, they must be created.
 */

class SellItemsCommand(private val walletManager: WalletManager) : CommandExecutor, TabCompleter {

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            return org.bukkit.Material.values()
                .map { it.key.toString() } // e.g. "minecraft:diamond"
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .sorted()
        }

        return emptyList()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player) {
            sender.sendMessage(Component.text("Only players can sell tokens!").color(TextColor.color(255, 0, 0)))
            return true
        }

        // Check if the command has the correct number of arguments
        if (args.size != 3) {
            sender.sendMessage(Component.text("Usage: /sell <item> <amount> <price>").color(TextColor.color(255, 0, 0)))
            return true
        }

        val itemName = args[0] // The item to sell
        val rawMaterialName = itemName.substringAfter(":").uppercase() // e.g., "DIAMOND"
        val material = Material.matchMaterial(itemName)
        if (material == null) {
            sender.sendMessage(Component.text("Unknown item type: $itemName").color(TextColor.color(255, 0, 0)))
            return true
        }

        val amount = args[1].toIntOrNull()
        val price = args[2] // The price per item if desired

        if (material == null) {
            sender.sendMessage(Component.text("Unknown item type: $itemName").color(TextColor.color(255, 0, 0)))
            return true
        }

        if (amount == null || amount <= 0) {
            sender.sendMessage(Component.text("Invalid amount.").color(TextColor.color(255, 0, 0)))
            return true
        }

        val senderUUID = sender.uniqueId

        val itemToRemove = ItemStack(material, amount)

        // Count total amount of that material in inventory
        val totalInInventory = sender.inventory.contents
            .filterNotNull()
            .filter { it.type == material }
            .sumOf { it.amount }

        if (totalInInventory < amount) {
            sender.sendMessage(Component.text("You don’t have enough $rawMaterialName to sell.").color(TextColor.color(255, 0, 0)))
            return true
        }

        // If enough, remove the exact amount manually
        var amountToRemove = amount!!
        val inventory = sender.inventory

        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot)
            if (item != null && item.type == material) {
                if (item.amount <= amountToRemove) {
                    amountToRemove -= item.amount
                    inventory.setItem(slot, null)
                } else {
                    item.amount -= amountToRemove
                    inventory.setItem(slot, item)
                    amountToRemove = 0
                }
            }
            if (amountToRemove <= 0) break
        }

        // if the item is not a token, create the token and execute the mint function
        val name = material.key.key.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        val symbol = material.name.take(4).uppercase() // e.g., "DIAM" for "DIAMOND"


        // this function should be used in the event that:
        // there is no minecraft asset created by the factory that matches the required item (e.g. diamond, DIAM)
        val alreadyExists = walletManager.tokenizeItem.checkAssetExists(name, symbol)

        if (!alreadyExists) {
            val newAddress = walletManager.tokenizeItem.createAsset(name, symbol)
            if (newAddress != null) {
                sender.sendMessage(Component.text("✅ Created new token for $rawMaterialName").color(TextColor.color(0, 255, 0)))
                // Optionally: save to assets.json here using a new helper
                walletManager.tokenizeItem.saveAsset(name, symbol, newAddress)
            } else {
                sender.sendMessage(Component.text("❌ Failed to create token for $rawMaterialName").color(TextColor.color(255, 0, 0)))
                return true
            }
        } else {
            sender.sendMessage(Component.text("ℹ️ Token already exists for $rawMaterialName").color(TextColor.color(200, 200, 0)))
            val assetAddress = walletManager.tokenizeItem.getAssetAddress(name, symbol)
            sender.sendMessage(Component.text("ℹ️ $rawMaterialName address: $assetAddress").color(TextColor.color(200, 200, 0)))
        }



        return true
    }
}
