package org.oreo.trade_plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.oreo.trade_plugin.commands.TradeCommand;

public final class TradePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Trade plugin is on");

        getCommand("trade").setExecutor(new TradeCommand(this)); // Register a command
    }
}
