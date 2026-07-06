package me.neoblade298.ashvote.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.rewards.RewardGroup;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdVoteAdminGivereward extends Subcommand {

    public CmdVoteAdminGivereward(String key, String desc, String perm, SubcommandRunner runner) {
        super(key, desc, perm, runner);
        this.args.add(new Arg("player", true));
        this.args.add(new Arg("group", true));
        this.overrideTabHandler();
    }

    @Override
    public void run(CommandSender s, String[] args) {
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            s.sendMessage("§cPlayer not found or not online.");
            return;
        }

        RewardGroup group = AshVote.inst().getRewardManager().getGroup(args[1]);
        if (group == null) {
            s.sendMessage("§cUnknown reward group: " + args[1]);
            return;
        }

        AshVote.inst().getRewardManager().giveReward(target, group);
        s.sendMessage("§aGave reward group '" + args[1] + "' to " + target.getName() + ".");
    }

    @Override
    public List<String> getTabOptions(CommandSender s, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            return AshVote.inst().getRewardManager().getGroupIds().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
