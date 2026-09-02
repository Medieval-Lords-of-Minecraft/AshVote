package me.neoblade298.ashvote.rewards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.player.VotePlayerData;

public class RewardManager {

    private final Map<String, RewardGroup> groups = new HashMap<>();
    private final Map<String, RewardTriggerEntry> triggers = new HashMap<>();
    private final Random random = new Random();
    private final AshVote plugin;

    public RewardManager(AshVote plugin) {
        this.plugin = plugin;
    }

    public void clear() {
        groups.clear();
        triggers.clear();
    }

    public void register(RewardGroup group) {
        groups.put(group.getId(), group);
    }

    public void registerTrigger(RewardTriggerEntry trigger) {
        triggers.put(trigger.getId(), trigger);
    }

    public RewardGroup getGroup(String id) {
        return groups.get(id);
    }

    public Set<String> getGroupIds() {
        return groups.keySet();
    }

    public RewardTriggerEntry getTrigger(String id) {
        return triggers.get(id);
    }

    public Set<String> getTriggerIds() {
        return triggers.keySet();
    }

    public boolean isGroup(String entry) {
        return !entry.contains(" ") && groups.containsKey(entry);
    }

    /**
     * Process all triggers for a player after a vote.
     * @param player the voting player
     * @param data the player's vote data
     */
    public void processRewards(Player player, VotePlayerData data) {
        for (RewardTriggerEntry trigger : triggers.values()) {
            processTrigger(player, data, trigger);
        }
    }

    /**
     * Manually run a specific reward group for a player (admin command).
     * Ignores triggers and gating.
     */
    public void giveReward(Player player, RewardGroup group) {
        executeRewards(player, group);
    }

    private void processTrigger(Player player, VotePlayerData data, RewardTriggerEntry trigger) {
        // Check permission
        if (trigger.getPermission() != null && !player.hasPermission(trigger.getPermission())) {
            return;
        }

        // Check firing condition
        boolean shouldFire;
        if (trigger.getWhen().getType() == RewardTriggerType.ALL_SITES) {
            // ALL_SITES requires extended context
            shouldFire = trigger.getWhen().shouldFireAllSites(data, plugin);
        } else {
            shouldFire = trigger.getWhen().shouldFire(data.getTotalVotes(), data.getStreak());
        }

        if (!shouldFire) {
            return;
        }

        // Roll chance gate before consuming a claim
        if (trigger.hasChance() && random.nextInt(100) >= trigger.getChance()) {
            return;
        }

        // Check max claims (keyed by trigger id)
        if (trigger.hasMaxClaims()) {
            int claimed = data.getClaimCount(trigger.getId());
            if (claimed >= trigger.getMaxClaims()) {
                return;
            }
            data.incrementClaimCount(trigger.getId());
        }

        executeEntry(player, trigger.getReward());

        // Mark all-sites reward as claimed for today
        if (trigger.getWhen().getType() == RewardTriggerType.ALL_SITES) {
            data.setLastAllSitesClaimDay(java.time.LocalDate.now());
        }
    }

    private void executeRewards(Player player, RewardGroup group) {
        if (group.hasChoices()) {
            String entry = pickWeighted(group.getChoices());
            if (entry != null) {
                executeEntry(player, entry);
            }
            return;
        }

        if (group.hasPermissioned()) {
            String entry = pickPermissioned(player, group.getPermissioned());
            if (entry != null) {
                executeEntry(player, entry);
            }
            return;
        }

        for (String entry : group.getRewards()) {
            executeEntry(player, entry);
        }
    }

    private void executeEntry(Player player, String entry) {
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

    private String pickWeighted(List<WeightedChoice> choices) {
        int total = 0;
        for (WeightedChoice c : choices) {
            total += c.getWeight();
        }
        if (total <= 0) {
            return null;
        }

        int roll = random.nextInt(total);
        for (WeightedChoice c : choices) {
            roll -= c.getWeight();
            if (roll < 0) {
                return c.getReward();
            }
        }
        return null;
    }

    /**
     * Resolve a permissioned entry list: the first entry whose permission the player
     * holds (or a 'default' entry) is chosen. Returns null if nothing matches.
     */
    private String pickPermissioned(Player player, List<PermissionedChoice> entries) {
        for (PermissionedChoice pc : entries) {
            if (pc.isDefault() || player.hasPermission(pc.getPermission())) {
                return pc.getReward();
            }
        }
        return null;
    }
}
