package me.neoblade298.ashtemplate.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.neoblade298.ashtemplate.player.PlayerData;
import me.neoblade298.ashtemplate.player.PlayerManager;

public class CmdExample implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6/at example §7- An example command");
            return true;
        }

        if (args[0].equalsIgnoreCase("example")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cThis command can only be run by a player!");
                return true;
            }
            PlayerData data = PlayerManager.get(p);
            if (data == null) {
                p.sendMessage("§cYour data hasn't loaded yet!");
                return true;
            }
            data.incrementCounter();
            p.sendMessage("Your counter is now: " + data.getCounter());
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use /at for a list of commands.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("example".startsWith(args[0].toLowerCase())) {
                completions.add("example");
            }
        }
        return completions;
    }
}
