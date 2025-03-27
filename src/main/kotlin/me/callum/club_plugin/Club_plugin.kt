package me.callum.club_plugin
import me.callum.club_plugin.commands.createwallet
import me.callum.club_plugin.commands.bal
import org.bukkit.plugin.java.JavaPlugin

class Club_plugin : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        logger.info("hello");
        registerCommands();
    }

    private fun registerCommands() {
        getCommand("createwallet")?.setExecutor(createwallet());
        getCommand("bal")?.setExecutor(bal());
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("goodbye");
    }
}
