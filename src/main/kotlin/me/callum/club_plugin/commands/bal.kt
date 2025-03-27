package me.callum.club_plugin.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class bal : CommandExecutor{
    private val client = HttpClient.newHttpClient()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            sender.sendMessage(Component.text("Querying balance...").color(TextColor.color(0, 255, 0)))

            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("https://testnet.qutblockchain.club"))
                    .build()

                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                sender.sendMessage(Component.text(response.body()).color(TextColor.color(0, 255, 0)))

                return true
            } catch (e: Exception) {
                sender.sendMessage(Component.text("Error querying balance: ${e.message}").color(TextColor.color(255, 0, 0)))
                return true
            }
        }
        return false
    }
}