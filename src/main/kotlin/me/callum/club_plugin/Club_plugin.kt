package me.callum.club_plugin
import me.callum.club_plugin.commands.*
import me.callum.club_plugin.commands.amm.add
import me.callum.club_plugin.commands.amm.remove
import org.bukkit.plugin.java.JavaPlugin
import me.callum.club_plugin.economy.WalletManager

class Club_plugin : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        logger.info("hello");
        registerCommands();
        registerEvents();
    }

    private fun registerCommands() {
        getCommand("bal")?.setExecutor(Bal());
        getCommand("send")?.setExecutor(SendTokensCommand())
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(WalletManager, this)  // Register WalletManager
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("goodbye");
    }
}
