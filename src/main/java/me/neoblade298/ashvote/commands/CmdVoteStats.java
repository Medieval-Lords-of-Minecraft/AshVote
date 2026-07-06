package me.neoblade298.ashvote.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.ashvote.player.PlayerManager;
import me.neoblade298.ashvote.player.VotePlayerData;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdVoteStats extends Subcommand {

    public CmdVoteStats(String key, String desc, String perm, SubcommandRunner runner) {
        super(key, desc, perm, runner);
        this.args.add(new Arg("player", false));
        this.overrideTabHandler();
    }

    @Override
    public void run(CommandSender s, String[] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                s.sendMessage("§cPlayer not found or not online.");
                return;
            }
        } else {
            target = (Player) s;
        }

        VotePlayerData data = PlayerManager.get(target);
        if (data == null) {
            s.sendMessage("§cData not loaded for " + target.getName() + ".");
            return;
        }

        s.sendMessage("§6§l=== Vote Stats: " + target.getName() + " ===");
        s.sendMessage("§7Total Votes: §f" + data.getTotalVotes());
        s.sendMessage("§7Monthly Votes: §f" + data.getMonthlyVotes());
        s.sendMessage("§7Current Streak: §f" + data.getStreak());

        long lastVote = data.getLastVoteTime();
        if (lastVote > 0) {
            long hoursAgo = (System.currentTimeMillis() - lastVote) / (60 * 60 * 1000L);
            s.sendMessage("§7Last Vote: §f" + hoursAgo + " hours ago");
        } else {
            s.sendMessage("§7Last Vote: §fNever");
        }
    }

    @Override
    public List<String> getTabOptions(CommandSender s, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
