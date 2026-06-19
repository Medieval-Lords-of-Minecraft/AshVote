package me.neoblade298.ashvote.rewards;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.neoblade298.ashvote.player.VotePlayerData;

public class RewardManager {

    private final Map<String, RewardGroup> groups = new HashMap<>();

    public void clear() {
        groups.clear();
    }

    public void register(RewardGroup group) {
        groups.put(group.getId(), group);
    }

    public RewardGroup getGroup(String id) {
        return groups.get(id);
    }

    public Set<String> getGroupIds() {
        return groups.keySet();
    }

    public boolean isGroup(String entry) {
        return !entry.contains(" ") && groups.containsKey(entry);
    }

    /**
     * Process all rewards for a player after a vote.
     * @param player the voting player
     * @param data the player's vote data
     */
    public void processRewards(Player player, VotePlayerData data) {
        for (RewardGroup group : groups.values()) {
            processGroup(player, data, group);
        }
    }

    /**
     * Manually trigger a specific reward group for a player (admin command).
     * Ignores trigger conditions and max-claims.
     */
    public void giveReward(Player player, RewardGroup group) {
        executeRewards(player, group);
    }

    private void processGroup(Player player, VotePlayerData data, RewardGroup group) {
        // Check permission
        if (group.getPermission() != null && !player.hasPermission(group.getPermission())) {
            return;
        }

        // Check trigger
        if (!group.getTrigger().shouldFire(data.getTotalVotes(), data.getStreak())) {
            return;
        }

        // Check max claims
        if (group.hasMaxClaims()) {
            int claimed = data.getClaimCount(group.getId());
            if (claimed >= group.getMaxClaims()) {
                return;
            }
            data.incrementClaimCount(group.getId());
        }

        executeRewards(player, group);
    }

    private void executeRewards(Player player, RewardGroup group) {
        for (String entry : group.getRewards()) {
            if (isGroup(entry)) {
                // Nested group reference
                RewardGroup nested = groups.get(entry);
                if (nested != null) {
                    executeRewards(player, nested);
                }
            } else {
                // Console command
                String command = entry.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }
}
