package me.neoblade298.ashvote.sites;

public class VoteSite {

    private final String id;
    private final String displayName;
    private final String url;
    private final String serviceName;
    private final SiteCooldownType cooldownType;

    public VoteSite(String id, String displayName, String url, String serviceName, SiteCooldownType cooldownType) {
        this.id = id;
        this.displayName = displayName;
        this.url = url;
        this.serviceName = serviceName;
        this.cooldownType = cooldownType;
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

    /**
     * Check if the given last vote time is still on cooldown.
     */
    public boolean isOnCooldown(long lastVoteTime) {
        if (lastVoteTime <= 0) return false;
        long now = System.currentTimeMillis();

        if (cooldownType == SiteCooldownType.TIMED) {
            return (now - lastVoteTime) < 24 * 60 * 60 * 1000L;
        } else {
            // FIXED_DAILY: on cooldown if last vote was today (same calendar day)
            java.time.LocalDate lastDate = java.time.Instant.ofEpochMilli(lastVoteTime)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            java.time.LocalDate today = java.time.LocalDate.now();
            return !lastDate.isBefore(today);
        }
    }
}
