package me.neoblade298.ashvote.config;

import java.io.File;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ConfigManager {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final AshVote plugin;
    private String canVoteAvailableMessage;
    private String canVoteUnavailableMessage;
    private String canVoteAllUnavailableMessage;

    public ConfigManager(AshVote plugin) {
        this.plugin = plugin;
    }

    public void reload(SiteManager siteManager, RewardManager rewardManager) {
        loadConfig();
        loadSites(siteManager);
        loadRewards(rewardManager);
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        canVoteAvailableMessage = parseMiniMessage(cfg.getString(
                "placeholders.canvote.available", "<green>Vote available!</green>"));
        canVoteUnavailableMessage = parseMiniMessage(cfg.getString(
                "placeholders.canvote.unavailable", "<gray>Thanks for voting!</gray>"));
        canVoteAllUnavailableMessage = parseMiniMessage(cfg.getString(
                "placeholders.canvote.all-unavailable", "<gray>Thanks for voting!</gray>"));
    }

    private String parseMiniMessage(String message) {
        return LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(message));
    }

    public String getCanVoteAvailableMessage() {
        return canVoteAvailableMessage;
    }

    public String getCanVoteUnavailableMessage() {
        return canVoteUnavailableMessage;
    }

    public String getCanVoteAllUnavailableMessage() {
        return canVoteAllUnavailableMessage;
    }

    private void loadSites(SiteManager siteManager) {
        siteManager.clear();
        File file = new File(plugin.getDataFolder(), "sites.yml");
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

        loadTriggers(cfg.getConfigurationSection("triggers"), rewardManager, plugin.getLogger());

        plugin.getLogger().info("Loaded " + rewardManager.getGroupIds().size() + " reward groups and "
                + rewardManager.getTriggerIds().size() + " triggers.");
    }

    static void loadTriggers(ConfigurationSection triggersSec, RewardManager rewardManager, Logger logger) {
        if (triggersSec != null) {
            for (String id : triggersSec.getKeys(false)) {
                ConfigurationSection sec = triggersSec.getConfigurationSection(id);
                if (sec == null) {
                    logger.warning("Trigger '" + id + "' must be a configuration section; skipping.");
                    continue;
                }
                try {
                    rewardManager.registerTrigger(parseTriggerEntry(sec, id));
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid trigger '" + id + "': " + e.getMessage() + "; skipping.");
                }
            }
        }
    }

    private RewardGroup parseGroup(ConfigurationSection sec, String id) {
        List<String> rewards = sec.getStringList("rewards");
        List<WeightedChoice> choices = parseChoices(sec, id);
        List<PermissionedChoice> permissioned = parsePermissioned(sec, id);
        List<String> worlds = sec.getStringList("worlds");

        int modes = 0;
        if (!choices.isEmpty()) modes++;
        if (!permissioned.isEmpty()) modes++;
        if (!rewards.isEmpty()) modes++;
        if (modes > 1) {
            plugin.getLogger().warning("Reward group '" + id
                    + "' defines more than one of 'choices', 'permissioned', 'rewards'; "
                    + "precedence is choices > permissioned > rewards.");
        }

        return new RewardGroup(id, rewards, choices, permissioned, worlds);
    }

    static RewardTriggerEntry parseTriggerEntry(ConfigurationSection sec, String id) {
        validateKeys(sec, Set.of("reward", "when", "permission", "max-claims", "chance"));
        String reward = requireString(sec, "reward");

        RewardTrigger when = parseCondition(sec);
        String permission = sec.contains("permission") ? requireString(sec, "permission") : null;
        int maxClaims = sec.contains("max-claims")
                ? requireInteger(sec, "max-claims", -1, Integer.MAX_VALUE) : -1;
        if (maxClaims == 0) {
            throw new IllegalArgumentException("'max-claims' must be positive or -1 (unlimited)");
        }
        int chance = sec.contains("chance") ? requireInteger(sec, "chance", 0, 100) : 100;

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

    private static RewardTrigger parseCondition(ConfigurationSection sec) {
        if (!sec.contains("when")) {
            return RewardTrigger.single(); // default: fire on every vote
        }

        ConfigurationSection whenSec = sec.getConfigurationSection("when");
        String typeName = whenSec == null ? requireString(sec, "when") : requireString(whenSec, "type");
        RewardTriggerType type;
        try {
            type = RewardTriggerType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown 'when' type '" + typeName + "'");
        }

        Set<String> fields = switch (type) {
            case SINGLE, ALL_SITES -> Set.of("type");
            case REPEATING -> Set.of("type", "interval");
            case STREAK, TOTAL -> Set.of("type", "value");
            case STREAK_CYCLE -> Set.of("type", "start", "interval");
        };
        if (whenSec == null) {
            if (type != RewardTriggerType.SINGLE && type != RewardTriggerType.ALL_SITES) {
                throw new IllegalArgumentException("'" + type + "' requires a 'when' section with 'type' and "
                        + "its parameters nested inside it");
            }
        } else {
            validateKeys(whenSec, fields);
        }

        return switch (type) {
            case SINGLE -> RewardTrigger.single();
            case REPEATING -> RewardTrigger.repeating(requireInteger(whenSec, "interval", 1, Integer.MAX_VALUE));
            case STREAK -> RewardTrigger.streak(requireInteger(whenSec, "value", 1, Integer.MAX_VALUE));
            case STREAK_CYCLE -> RewardTrigger.streakCycle(
                    requireInteger(whenSec, "start", 1, Integer.MAX_VALUE),
                    requireInteger(whenSec, "interval", 1, Integer.MAX_VALUE));
            case TOTAL -> RewardTrigger.total(requireInteger(whenSec, "value", 1, Integer.MAX_VALUE));
            case ALL_SITES -> RewardTrigger.allSites();
        };
    }

    private static String requireString(ConfigurationSection sec, String key) {
        if (!(sec.get(key) instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("'" + key + "' must be a non-empty string");
        }
        return value;
    }

    private static int requireInteger(ConfigurationSection sec, String key, int min, int max) {
        if (!sec.isInt(key)) {
            throw new IllegalArgumentException("'" + key + "' must be an integer in [" + min + ", " + max + "]");
        }
        int value = sec.getInt(key);
        if (value < min || value > max) {
            throw new IllegalArgumentException("'" + key + "' must be in [" + min + ", " + max + "]");
        }
        return value;
    }

    private static void validateKeys(ConfigurationSection sec, Set<String> allowed) {
        for (String key : sec.getKeys(false)) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("unexpected field '" + key + "' in '" + sec.getCurrentPath()
                        + "'; condition parameters belong inside 'when' and must match its type");
            }
        }
    }

}
