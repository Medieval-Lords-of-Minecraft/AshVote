package me.neoblade298.ashvote.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.rewards.RewardGroup;
import me.neoblade298.ashvote.rewards.RewardManager;
import me.neoblade298.ashvote.rewards.RewardTrigger;
import me.neoblade298.ashvote.rewards.RewardTriggerType;
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

        for (String id : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(id);
            if (sec == null) continue;

            List<String> rewards = sec.getStringList("rewards");
            String permission = sec.getString("permission", null);
            int maxClaims = sec.getInt("max-claims", -1);
            RewardTrigger trigger = parseTrigger(sec);

            rewardManager.register(new RewardGroup(id, rewards, trigger, permission, maxClaims));
        }

        plugin.getLogger().info("Loaded " + rewardManager.getGroupIds().size() + " reward groups.");
    }

    private RewardTrigger parseTrigger(ConfigurationSection sec) {
        if (!sec.contains("trigger")) {
            return RewardTrigger.single();
        }

        // Simple string trigger (e.g. trigger: SINGLE)
        if (sec.isString("trigger")) {
            String type = sec.getString("trigger", "SINGLE").toUpperCase();
            if (type.equals("SINGLE")) return RewardTrigger.single();
            // Other types require params, default to single
            return RewardTrigger.single();
        }

        // Object trigger
        ConfigurationSection triggerSec = sec.getConfigurationSection("trigger");
        if (triggerSec == null) return RewardTrigger.single();

        RewardTriggerType type;
        try {
            type = RewardTriggerType.valueOf(triggerSec.getString("type", "SINGLE").toUpperCase());
        } catch (IllegalArgumentException e) {
            return RewardTrigger.single();
        }

        return switch (type) {
            case SINGLE -> RewardTrigger.single();
            case REPEATING -> RewardTrigger.repeating(triggerSec.getInt("interval", 1));
            case STREAK -> RewardTrigger.streak(triggerSec.getInt("value", 1));
            case STREAK_CYCLE -> RewardTrigger.streakCycle(triggerSec.getInt("start", 1), triggerSec.getInt("interval", 1));
            case TOTAL -> RewardTrigger.total(triggerSec.getInt("value", 1));
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
