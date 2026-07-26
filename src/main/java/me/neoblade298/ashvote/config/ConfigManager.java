package me.neoblade298.ashvote.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.rewards.PermissionedChoice;
import me.neoblade298.ashvote.rewards.RewardGroup;
import me.neoblade298.ashvote.rewards.RewardManager;
import me.neoblade298.ashvote.rewards.RewardTrigger;
import me.neoblade298.ashvote.rewards.RewardTriggerEntry;
import me.neoblade298.ashvote.rewards.RewardTriggerType;
import me.neoblade298.ashvote.rewards.WeightedChoice;
import me.neoblade298.ashvote.sites.SiteCooldownType;
import me.neoblade298.ashvote.sites.SiteManager;
import me.neoblade298.ashvote.sites.VoteSite;

public class ConfigManager {

    private final AshVote plugin;

    public ConfigManager(AshVote plugin) {
        this.plugin = plugin;
    }

    public void reload(SiteManager siteManager, RewardManager rewardManager) {
        loadSites(siteManager);
        loadRewards(rewardManager);
    }

    private void loadSites(SiteManager siteManager) {
        siteManager.clear();
        File file = new File(plugin.getDataFolder(), "sites.yml");
        ensureResource("sites.yml", file);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection sites = cfg.getConfigurationSection("sites");
        if (sites == null) return;

        for (String id : sites.getKeys(false)) {
            ConfigurationSection sec = sites.getConfigurationSection(id);
            if (sec == null) continue;

            String displayName = sec.getString("display-name", id);
            String url = sec.getString("url", "");
            String serviceName = sec.getString("service-name", id);
            SiteCooldownType cooldownType;
            try {
                cooldownType = SiteCooldownType.valueOf(sec.getString("cooldown-type", "TIMED").toUpperCase());
            } catch (IllegalArgumentException e) {
                cooldownType = SiteCooldownType.TIMED;
            }

            ZoneId timezone;
            String tzString = sec.getString("timezone", null);
            if (tzString == null || tzString.isBlank()) {
                timezone = ZoneId.systemDefault();
            } else {
                try {
                    timezone = ZoneId.of(tzString);
                } catch (DateTimeException e) {
                    plugin.getLogger().warning("Invalid timezone '" + tzString + "' for site '" + id
                            + "', falling back to server default (" + ZoneId.systemDefault() + ").");
                    timezone = ZoneId.systemDefault();
                }
            }

            siteManager.register(new VoteSite(id, displayName, url, serviceName, cooldownType, timezone));
        }

        plugin.getLogger().info("Loaded " + siteManager.getAll().size() + " vote sites.");
    }

    private void loadRewards(RewardManager rewardManager) {
        rewardManager.clear();
        File file = new File(plugin.getDataFolder(), "rewards.yml");
        ensureResource("rewards.yml", file);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Reward groups: pure "what you get" definitions. They never fire on their own.
        ConfigurationSection groupsSec = cfg.getConfigurationSection("groups");
        if (groupsSec != null) {
            for (String id : groupsSec.getKeys(false)) {
                ConfigurationSection sec = groupsSec.getConfigurationSection(id);
                if (sec == null) continue;
                rewardManager.register(parseGroup(sec, id));
            }
        }

        // Triggers: the "when + gating" entries evaluated on each vote.
        ConfigurationSection triggersSec = cfg.getConfigurationSection("triggers");
        if (triggersSec != null) {
            for (String id : triggersSec.getKeys(false)) {
                ConfigurationSection sec = triggersSec.getConfigurationSection(id);
                if (sec == null) continue;
                RewardTriggerEntry trigger = parseTriggerEntry(sec, id);
                if (trigger != null) rewardManager.registerTrigger(trigger);
            }
        }

        plugin.getLogger().info("Loaded " + rewardManager.getGroupIds().size() + " reward groups and "
                + rewardManager.getTriggerIds().size() + " triggers.");
    }

    private RewardGroup parseGroup(ConfigurationSection sec, String id) {
        List<String> rewards = sec.getStringList("rewards");
        List<WeightedChoice> choices = parseChoices(sec, id);
        List<PermissionedChoice> permissioned = parsePermissioned(sec, id);

        int modes = 0;
        if (!choices.isEmpty()) modes++;
        if (!permissioned.isEmpty()) modes++;
        if (!rewards.isEmpty()) modes++;
        if (modes > 1) {
            plugin.getLogger().warning("Reward group '" + id
                    + "' defines more than one of 'choices', 'permissioned', 'rewards'; "
                    + "precedence is choices > permissioned > rewards.");
        }

        return new RewardGroup(id, rewards, choices, permissioned);
    }

    private RewardTriggerEntry parseTriggerEntry(ConfigurationSection sec, String id) {
        String reward = sec.getString("reward", null);
        if (reward == null || reward.isBlank()) {
            plugin.getLogger().warning("Trigger '" + id + "' is missing a 'reward'; skipping.");
            return null;
        }

        RewardTrigger when = parseCondition(sec);
        String permission = sec.getString("permission", null);
        int maxClaims = sec.getInt("max-claims", -1);
        int chance = clampChance(sec.getInt("chance", 100), id);

        return new RewardTriggerEntry(id, reward, when, permission, maxClaims, chance);
    }

    private List<WeightedChoice> parseChoices(ConfigurationSection sec, String groupId) {
        if (!sec.contains("choices")) return List.of();

        List<WeightedChoice> result = new ArrayList<>();
        for (Map<?, ?> map : sec.getMapList("choices")) {
            Object rewardObj = map.get("reward");
            if (rewardObj == null) {
                plugin.getLogger().warning("Reward group '" + groupId + "' has a choice missing 'reward'; skipping.");
                continue;
            }

            int weight = 1;
            Object weightObj = map.get("weight");
            if (weightObj instanceof Number n) {
                weight = n.intValue();
            }
            if (weight <= 0) {
                plugin.getLogger().warning("Reward group '" + groupId + "' has a choice with weight <= 0; skipping.");
                continue;
            }

            result.add(new WeightedChoice(weight, rewardObj.toString()));
        }
        return result;
    }

    private List<PermissionedChoice> parsePermissioned(ConfigurationSection sec, String groupId) {
        if (!sec.contains("permissioned")) return List.of();

        List<PermissionedChoice> result = new ArrayList<>();
        for (Map<?, ?> map : sec.getMapList("permissioned")) {
            if (map.isEmpty()) {
                plugin.getLogger().warning("Reward group '" + groupId + "' has an empty permissioned entry; skipping.");
                continue;
            }

            // Each entry is a single-key map: 'permission: reward' (or 'default: reward').
            Map.Entry<?, ?> entry = map.entrySet().iterator().next();
            String permission = String.valueOf(entry.getKey());
            Object rewardObj = entry.getValue();
            if (rewardObj == null) {
                plugin.getLogger().warning("Reward group '" + groupId + "' has a permissioned entry '" + permission
                        + "' missing a reward; skipping.");
                continue;
            }

            boolean isDefault = permission.equalsIgnoreCase("default");
            result.add(new PermissionedChoice(isDefault ? null : permission, rewardObj.toString()));
        }
        return result;
    }

    private int clampChance(int chance, String groupId) {
        if (chance < 0 || chance > 100) {
            plugin.getLogger().warning("Reward group '" + groupId + "' has chance " + chance
                    + " outside [0, 100]; clamping.");
            return Math.max(0, Math.min(100, chance));
        }
        return chance;
    }

    private RewardTrigger parseCondition(ConfigurationSection sec) {
        if (!sec.contains("when")) {
            return RewardTrigger.single(); // default: fire on every vote
        }

        // Simple string condition (e.g. when: SINGLE)
        if (sec.isString("when")) {
            String type = sec.getString("when", "SINGLE").toUpperCase();
            if (type.equals("SINGLE")) return RewardTrigger.single();
            // Other types require params, default to single
            return RewardTrigger.single();
        }

        // Object condition (e.g. when: { type: REPEATING, interval: 7 })
        ConfigurationSection whenSec = sec.getConfigurationSection("when");
        if (whenSec == null) return RewardTrigger.single();

        RewardTriggerType type;
        try {
            type = RewardTriggerType.valueOf(whenSec.getString("type", "SINGLE").toUpperCase());
        } catch (IllegalArgumentException e) {
            return RewardTrigger.single();
        }

        return switch (type) {
            case SINGLE -> RewardTrigger.single();
            case REPEATING -> RewardTrigger.repeating(whenSec.getInt("interval", 1));
            case STREAK -> RewardTrigger.streak(whenSec.getInt("value", 1));
            case STREAK_CYCLE -> RewardTrigger.streakCycle(whenSec.getInt("start", 1), whenSec.getInt("interval", 1));
            case TOTAL -> RewardTrigger.total(whenSec.getInt("value", 1));
        };
    }

    private void ensureResource(String name, File file) {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource(name)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save default " + name + ": " + e.getMessage());
            }
        }
    }
}
