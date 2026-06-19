package me.neoblade298.ashtemplate;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import me.neoblade298.ashtemplate.commands.CmdExample;
import me.neoblade298.ashtemplate.player.PlayerManager;
import me.neoblade298.neocore.bukkit.NeoCore;

public class AshTemplate extends JavaPlugin {

    private static AshTemplate inst;

    public static AshTemplate inst() {
        return inst;
    }

    @Override
    public void onEnable() {
        inst = this;

        // Register IO component for player data load/save
        NeoCore.registerIOComponent(this, new PlayerManager(), "AshTemplate-PlayerManager");

        // Register commands
        initCommands();

        getLogger().info("AshTemplate enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AshTemplate disabled!");
    }

    private void initCommands() {
        PluginCommand atCmd = getCommand("at");
        CmdExample executor = new CmdExample();
        atCmd.setExecutor(executor);
        atCmd.setTabCompleter(executor);
    }
}
