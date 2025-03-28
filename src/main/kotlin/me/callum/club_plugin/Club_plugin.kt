package me.callum.club_plugin
import me.callum.club_plugin.commands.buy
import me.callum.club_plugin.commands.checkprice
import me.callum.club_plugin.commands.pay
import me.callum.club_plugin.commands.sell
import me.callum.club_plugin.commands.amm.add
import me.callum.club_plugin.commands.amm.remove
import me.callum.club_plugin.commands.Bal
import org.bukkit.plugin.java.JavaPlugin

class Club_plugin : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        logger.info("hello");
        registerCommands();
    }

    private fun registerCommands() {
        getCommand("bal")?.setExecutor(Bal());
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("goodbye");
    }
}
