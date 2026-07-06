package me.neoblade298.ashvote.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;

public class CmdVoteAdminFakevote extends Subcommand {

    public CmdVoteAdminFakevote(String key, String desc, String perm, SubcommandRunner runner) {
        super(key, desc, perm, runner);
        this.args.add(new Arg("player", true));
        this.args.add(new Arg("site", false));
        this.overrideTabHandler();
    }

    @Override
    public void run(CommandSender s, String[] args) {
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            s.sendMessage("§cPlayer not found or not online.");
            return;
        }

        String serviceName;
        if (args.length > 1) {
            var site = AshVote.inst().getSiteManager().getById(args[1]);
            if (site == null) {
                s.sendMessage("§cUnknown site id: " + args[1]);
                return;
            }
            serviceName = site.getServiceName();
        } else {
            // Use the first configured site
            var sites = AshVote.inst().getSiteManager().getAll();
            if (sites.isEmpty()) {
                s.sendMessage("§cNo vote sites configured.");
                return;
            }
            serviceName = sites.iterator().next().getServiceName();
        }

        AshVote.inst().getVoteListener().processVote(target, serviceName);
        s.sendMessage("§aFake vote sent for " + target.getName() + " (service: " + serviceName + ").");
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
            return AshVote.inst().getSiteManager().getAll().stream()
                    .map(site -> site.getId())
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
