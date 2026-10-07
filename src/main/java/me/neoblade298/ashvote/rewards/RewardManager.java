package me.neoblade298.ashvote.rewards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.player.VotePlayerData;
import me.neoblade298.ashvote.sites.VoteSite;

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
    public void processRewards(Player player, VotePlayerData data, VoteSite site) {
        for (RewardTriggerEntry trigger : triggers.values()) {
            processTrigger(player, data, site, trigger);
        }
    }

    /**
     * Manually run a specific reward group for a player (admin command).
     * Ignores triggers and gating.
     */
    public void giveReward(Player player, RewardGroup group) {
        executeRewards(player, group, null);
    }

    private void processTrigger(Player player, VotePlayerData data, VoteSite site, RewardTriggerEntry trigger) {
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

        executeEntry(player, site, trigger.getReward());

        // Mark all-sites reward as claimed for today
        if (trigger.getWhen().getType() == RewardTriggerType.ALL_SITES) {
            data.setLastAllSitesClaimDay(java.time.LocalDate.now());
        }
    }

    private void executeRewards(Player player, RewardGroup group, VoteSite site) {
        if (!group.isAvailableIn(player.getWorld().getName())) {
            VotePlayerData data = me.neoblade298.ashvote.player.PlayerManager.get(player);
            if (data == null) {
                plugin.getLogger().warning("Could not defer reward group '" + group.getId()
                        + "' for " + player.getName() + ": player data is not loaded.");
                return;
            }

            boolean alreadyPending = data.getPendingRewardCount(group.getId()) > 0;
            data.addPendingReward(group.getId());
            if (!alreadyPending) {
                player.sendMessage("§eA vote reward is waiting for you in an eligible world.");
            }
            return;
        }

        if (group.hasChoices()) {
            String entry = pickWeighted(group.getChoices());
            if (entry != null) {
                executeEntry(player, site, entry);
            }
            return;
        }

        if (group.hasPermissioned()) {
            String entry = pickPermissioned(player, group.getPermissioned());
            if (entry != null) {
                executeEntry(player, site, entry);
            }
            return;
        }

        for (String entry : group.getRewards()) {
            executeEntry(player, site, entry);
        }
    }

    public void deliverPendingRewards(Player player) {
        VotePlayerData data = me.neoblade298.ashvote.player.PlayerManager.get(player);
        if (data == null || data.getPendingRewards().isEmpty()) {
            return;
        }

        boolean delivered = false;
        for (Entry<String, Integer> pending : Map.copyOf(data.getPendingRewards()).entrySet()) {
            RewardGroup group = groups.get(pending.getKey());
            if (group == null) {
                plugin.getLogger().warning("Pending reward group '" + pending.getKey() + "' for "
                        + player.getName() + " is not configured; leaving it pending.");
                continue;
            }
            if (!group.isAvailableIn(player.getWorld().getName())) {
                continue;
            }

            for (int i = 0; i < pending.getValue(); i++) {
                executeRewards(player, group, null);
                data.consumePendingReward(group.getId());
                delivered = true;
            }
        }

        if (delivered) {
            player.sendMessage("§aYour pending vote rewards have been delivered.");
        }
    }

    private void executeEntry(Player player, VoteSite site, String entry) {
        if (isGroup(entry)) {
            // Nested group reference
            RewardGroup nested = groups.get(entry);
            if (nested != null) {
                executeRewards(player, nested, site);
            }
        } else {
            // Console command
            String command = entry.replace("%player%", player.getName());
            if (site != null) {
                command = command.replace("%site%", site.getId());
            }
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
