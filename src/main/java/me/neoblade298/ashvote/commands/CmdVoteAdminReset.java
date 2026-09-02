package me.neoblade298.ashvote.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.player.PlayerManager;
import me.neoblade298.ashvote.player.VotePlayerData;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdVoteAdminReset extends Subcommand {

    public CmdVoteAdminReset(String key, String desc, String perm, SubcommandRunner runner) {
        super(key, desc, perm, runner);
        this.args.add(new Arg("player", true));
        this.overrideTabHandler();
    }

    @Override
    public void run(CommandSender s, String[] args) {
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            s.sendMessage("§cPlayer not found or not online.");
            return;
        }

        VotePlayerData data = PlayerManager.get(target);
        if (data == null) {
            s.sendMessage("§cData not loaded for " + target.getName() + ".");
            return;
        }

        data.resetVotes();
        AshVote.inst().getLeaderboardManager().refreshAll();
        s.sendMessage("§aReset all vote data for " + target.getName() + ".");
    }

    @Override
    public List<String> getTabOptions(CommandSender s, String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}