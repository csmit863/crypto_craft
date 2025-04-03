package me.callum.club_plugin
import me.callum.club_plugin.commands.*
import me.callum.club_plugin.commands.amm.add
import me.callum.club_plugin.commands.amm.remove
import me.callum.club_plugin.economy.Blockcoin
import org.bukkit.plugin.java.JavaPlugin
import me.callum.club_plugin.economy.WalletManager

class Club_plugin : JavaPlugin() {

    private lateinit var blockcoin: Blockcoin
    private lateinit var walletManager: WalletManager  // Store the instance

    override fun onEnable() {
        // Plugin startup logic
        logger.info("hello");

        blockcoin = Blockcoin()
        walletManager = WalletManager(blockcoin)

        registerCommands();
        registerEvents();
    }

    private fun registerCommands() {
        getCommand("bal")?.setExecutor(Bal(walletManager));
        getCommand("send")?.setExecutor(SendTokensCommand(walletManager))
        getCommand("setToken")?.setExecutor(SetTokenCommand(blockcoin))
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(walletManager, this)  // Register WalletManager
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("goodbye");
    }
}
