package me.neoblade298.ashvote.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.leaderboard.LeaderboardEntry;
import me.neoblade298.ashvote.leaderboard.LeaderboardManager;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdVoteLeaderboard extends Subcommand {

    public CmdVoteLeaderboard(String key, String desc, String perm, SubcommandRunner runner) {
        super(key, desc, perm, runner);
    }

    @Override
    public void run(CommandSender s, String[] args) {
        LeaderboardManager lb = AshVote.inst().getLeaderboardManager();

        s.sendMessage("§6§l=== Monthly Top 10 ===");
        displayBoard(s, lb.getTopMonthly());

        s.sendMessage("§6§l=== All-Time Top 10 ===");
        displayBoard(s, lb.getTopAllTime());

        s.sendMessage("§6§l=== Top Streaks ===");
        displayBoard(s, lb.getTopStreaks());
    }

    private void displayBoard(CommandSender s, List<LeaderboardEntry> entries) {
        if (entries.isEmpty()) {
            s.sendMessage("  §7No entries yet.");
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            s.sendMessage("  §e#" + (i + 1) + " §f" + entry.getPlayerName() + " §7- §a" + entry.getValue());
        }
    }

    @Override
    public List<String> getTabOptions(CommandSender s, String[] args) {
        return List.of();
    }
}
