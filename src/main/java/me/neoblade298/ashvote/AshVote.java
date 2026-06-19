package me.neoblade298.ashvote;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import me.neoblade298.ashvote.commands.CmdExample;
import me.neoblade298.ashvote.player.PlayerManager;
import me.neoblade298.neocore.bukkit.NeoCore;

public class AshVote extends JavaPlugin {

    private static AshVote inst;

    public static AshVote inst() {
        return inst;
    }

    @Override
    public void onEnable() {
        inst = this;

        // Register IO component for player data load/save
        NeoCore.registerIOComponent(this, new PlayerManager(), "AshVote-PlayerManager");

        // Register commands
        initCommands();

        getLogger().info("AshVote enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AshVote disabled!");
    }

    private void initCommands() {
        PluginCommand avCmd = getCommand("av");
        CmdExample executor = new CmdExample();
        avCmd.setExecutor(executor);
        avCmd.setTabCompleter(executor);
    }
}
