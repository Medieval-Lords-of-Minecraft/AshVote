package me.neoblade298.ashvote.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdAshVoteDebug extends Subcommand {

    public CmdAshVoteDebug(String key, String desc, String perm, SubcommandRunner runner) {
        super(key, desc, perm, runner);
    }

    @Override
    public void run(CommandSender s, String[] args) {
        if (args.length != 0) {
            s.sendMessage("§cUsage: /ashvote debug");
            return;
        }

        boolean enabled = AshVote.inst().toggleDebugLogging();
        s.sendMessage("§aAshVote debug logging " + (enabled ? "enabled" : "disabled") + ".");
    }

    @Override
    public List<String> getTabOptions(CommandSender s, String[] args) {
        return List.of();
    }
}
