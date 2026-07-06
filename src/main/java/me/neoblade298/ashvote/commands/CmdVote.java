package me.neoblade298.ashvote.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.player.PlayerManager;
import me.neoblade298.ashvote.player.VotePlayerData;
import me.neoblade298.ashvote.sites.VoteSite;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class CmdVote extends Subcommand {

    public CmdVote(String key, String desc, String perm, SubcommandRunner runner) {
        super(key, desc, perm, runner);
    }

    @Override
    public void run(CommandSender s, String[] args) {
        Player p = (Player) s;
        VotePlayerData data = PlayerManager.get(p);

        p.sendMessage("§6§l=== Vote Links ===");

        for (VoteSite site : AshVote.inst().getSiteManager().getAll()) {
            long lastVote = data != null ? data.getSiteCooldown(site.getId()) : 0;
            boolean onCooldown = site.isOnCooldown(lastVote);

            Component msg;
            if (onCooldown) {
                msg = Component.text("  ✗ ", NamedTextColor.RED)
                        .append(Component.text(site.getDisplayName().replace("&", "§"), NamedTextColor.GRAY))
                        .append(Component.text(" (on cooldown)", NamedTextColor.RED));
            } else {
                msg = Component.text("  ✓ ", NamedTextColor.GREEN)
                        .append(Component.text(site.getDisplayName().replace("&", "§"), NamedTextColor.GREEN))
                        .append(Component.text(" [Click to Vote]", NamedTextColor.YELLOW)
                                .clickEvent(ClickEvent.openUrl(site.getUrl())));
            }
            p.sendMessage(msg);
        }
    }

    @Override
    public List<String> getTabOptions(CommandSender s, String[] args) {
        return List.of();
    }
}
