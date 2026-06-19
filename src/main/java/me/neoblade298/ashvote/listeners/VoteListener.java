package me.neoblade298.ashvote.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.player.PlayerManager;
import me.neoblade298.ashvote.player.VotePlayerData;
import me.neoblade298.ashvote.sites.SiteManager;
import me.neoblade298.ashvote.sites.VoteSite;

public class VoteListener implements Listener {

    private final AshVote plugin;

    public VoteListener(AshVote plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVote(VotifierEvent event) {
        Vote vote = event.getVote();
        String username = vote.getUsername();
        String serviceName = vote.getServiceName();

        plugin.getLogger().info("Vote received: " + username + " from " + serviceName);

        Player player = Bukkit.getPlayerExact(username);
        if (player == null || !player.isOnline()) {
            plugin.getLogger().info("Player " + username + " is not online, vote logged but not processed.");
            return;
        }

        processVote(player, serviceName);
    }

    public void processVote(Player player, String serviceName) {
        SiteManager siteManager = plugin.getSiteManager();
        VoteSite site = siteManager.getByService(serviceName);

        if (site == null) {
            plugin.getLogger().warning("Vote from unknown service: " + serviceName + " (player: " + player.getName() + ")");
            return;
        }

        VotePlayerData data = PlayerManager.get(player);
        if (data == null) {
            plugin.getLogger().warning("Player data not loaded for " + player.getName() + ", vote not processed.");
            return;
        }

        // Check site cooldown
        long lastSiteVote = data.getSiteCooldown(site.getId());
        if (site.isOnCooldown(lastSiteVote)) {
            plugin.getLogger().info("Vote from " + player.getName() + " on " + site.getId() + " is on cooldown, ignoring.");
            return;
        }

        // Record the vote
        data.setSiteCooldown(site.getId(), System.currentTimeMillis());
        data.recordVote();

        // Process rewards
        plugin.getRewardManager().processRewards(player, data);

        // Refresh leaderboard
        plugin.getLeaderboardManager().refreshAll();

        player.sendMessage("§aThanks for voting on " + site.getDisplayName().replace("&", "§") + "§a!");
        plugin.getLogger().info("Vote processed: " + player.getName() + " via " + site.getId() +
                " (total: " + data.getTotalVotes() + ", streak: " + data.getStreak() + ")");
    }
}
