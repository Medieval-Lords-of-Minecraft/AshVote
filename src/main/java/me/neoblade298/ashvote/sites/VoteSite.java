package me.neoblade298.ashvote.sites;

import java.time.ZoneId;

public class VoteSite {

    private final String id;
    private final String displayName;
    private final String url;
    private final String serviceName;
    private final SiteCooldownType cooldownType;
    private final ZoneId timezone;

    public VoteSite(String id, String displayName, String url, String serviceName, SiteCooldownType cooldownType, ZoneId timezone) {
        this.id = id;
        this.displayName = displayName;
        this.url = url;
        this.serviceName = serviceName;
        this.cooldownType = cooldownType;
        this.timezone = timezone;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUrl() {
        return url;
    }

    public String getServiceName() {
        return serviceName;
    }

    public SiteCooldownType getCooldownType() {
        return cooldownType;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    /**
     * Check if the given last vote time is still on cooldown.
     */
    public boolean isOnCooldown(long lastVoteTime) {
        if (lastVoteTime <= 0) return false;
        long now = System.currentTimeMillis();

        if (cooldownType == SiteCooldownType.TIMED) {
            return (now - lastVoteTime) < 24 * 60 * 60 * 1000L;
        } else {
            // FIXED_DAILY: on cooldown if last vote was today (same calendar day) in the site's timezone
            java.time.LocalDate lastDate = java.time.Instant.ofEpochMilli(lastVoteTime)
                    .atZone(timezone).toLocalDate();
            java.time.LocalDate today = java.time.LocalDate.now(timezone);
            return !lastDate.isBefore(today);
        }
    }
}
