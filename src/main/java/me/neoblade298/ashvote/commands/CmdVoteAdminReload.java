package me.neoblade298.ashvote.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdVoteAdminReload extends Subcommand {

    public CmdVoteAdminReload() {
        super("reload", "Reload configs", "ashvote.admin", SubcommandRunner.BOTH);
    }

    @Override
    public void run(CommandSender s, String[] args) {
        AshVote.inst().reloadConfigs();
        s.sendMessage("§aAshVote configs reloaded.");
    }

    @Override
    public List<String> getTabOptions(CommandSender s, String[] args) {
        return List.of();
    }
}
