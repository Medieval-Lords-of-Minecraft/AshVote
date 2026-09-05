package me.neoblade298.ashvote.listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

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
import me.neoblade298.neocore.bukkit.NeoCore;

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
        recordVote(username, serviceName);
    }

    public void recordVote(String username, String serviceName) {
        recordVote(username, serviceName, false);
    }

    public void recordVote(String username, String serviceName, boolean bypassCooldown) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String voteId = UUID.randomUUID().toString();
            try (Connection con = NeoCore.getConnection("AshVote");
                 PreparedStatement stmt = con.prepareStatement(
                         "INSERT INTO ashvote_votes (id, username, service_name, received_at, state, bypass_cooldown) " +
                         "VALUES (?, ?, ?, ?, 'PENDING', ?)")) {
                stmt.setString(1, voteId);
                stmt.setString(2, username);
                stmt.setString(3, serviceName);
                stmt.setLong(4, System.currentTimeMillis());
                stmt.setBoolean(5, bypassCooldown);
                stmt.executeUpdate();
                plugin.getLogger().info("Recorded vote " + voteId + " for " + username + " from " + serviceName + ".");

                PlayerManager.tryProcessVote(voteId, username, serviceName, bypassCooldown);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to record vote for " + username + " from " + serviceName + "!");
                e.printStackTrace();
            }
        });
    }

    public VoteResult processVote(Player player, String serviceName, boolean bypassCooldown) {
        SiteManager siteManager = plugin.getSiteManager();
        VoteSite site = siteManager.getByService(serviceName);

        if (site == null) {
            plugin.getLogger().warning("Vote from unknown service: " + serviceName + " (player: " + player.getName() + ")");
            return VoteResult.rejected("Unknown vote service");
        }

        VotePlayerData data = PlayerManager.get(player);
        if (data == null) {
            plugin.getLogger().warning("Player data not loaded for " + player.getName() + ", vote not processed.");
            return VoteResult.retry("Player data is not loaded");
        }

        // Check site cooldown
        long lastSiteVote = data.getSiteCooldown(site.getId());
        if (!bypassCooldown && site.isOnCooldown(lastSiteVote)) {
            plugin.getLogger().info("Vote from " + player.getName() + " on " + site.getId() + " is on cooldown, ignoring.");
            return VoteResult.rejected("Site cooldown is active");
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
        return VoteResult.processed();
    }

    public record VoteResult(String state, String detail) {
        public static VoteResult processed() {
            return new VoteResult("PROCESSED", null);
        }

        public static VoteResult rejected(String detail) {
            return new VoteResult("REJECTED", detail);
        }

        public static VoteResult retry(String detail) {
            return new VoteResult("PENDING", detail);
        }
    }
}
