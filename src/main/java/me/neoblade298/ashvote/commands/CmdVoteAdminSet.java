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

public class CmdVoteAdminSet extends Subcommand {

    public CmdVoteAdminSet() {
        super("set", "Set a player's vote stat", "ashvote.admin", SubcommandRunner.BOTH);
        this.args.add(new Arg("player", true));
        this.args.add(new Arg("total|streak|monthly", true));
        this.args.add(new Arg("value", true));
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

        int value;
        try {
            value = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            s.sendMessage("§cInvalid number: " + args[2]);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "total" -> {
                data.setTotalVotes(value);
                s.sendMessage("§aSet " + target.getName() + "'s total votes to " + value + ".");
            }
            case "streak" -> {
                data.setStreak(value);
                s.sendMessage("§aSet " + target.getName() + "'s streak to " + value + ".");
            }
            case "monthly" -> {
                data.setMonthlyVotes(value);
                s.sendMessage("§aSet " + target.getName() + "'s monthly votes to " + value + ".");
            }
            default -> s.sendMessage("§cUnknown stat: " + args[1] + ". Use total, streak, or monthly.");
        }
    }

    @Override
    public List<String> getTabOptions(CommandSender s, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            return List.of("total", "streak", "monthly").stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
