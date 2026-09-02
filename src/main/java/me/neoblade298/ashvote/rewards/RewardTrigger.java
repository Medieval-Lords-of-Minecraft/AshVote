package me.neoblade298.ashvote.rewards;

import me.neoblade298.ashvote.player.VotePlayerData;

public class RewardTrigger {

    private final RewardTriggerType type;
    private final int value;
    private final int interval;

    private RewardTrigger(RewardTriggerType type, int value, int interval) {
        this.type = type;
        this.value = value;
        this.interval = interval;
    }

    public static RewardTrigger single() {
        return new RewardTrigger(RewardTriggerType.SINGLE, 0, 0);
    }

    public static RewardTrigger repeating(int interval) {
        return new RewardTrigger(RewardTriggerType.REPEATING, 0, interval);
    }

    public static RewardTrigger streak(int value) {
        return new RewardTrigger(RewardTriggerType.STREAK, value, 0);
    }

    public static RewardTrigger streakCycle(int start, int interval) {
        return new RewardTrigger(RewardTriggerType.STREAK_CYCLE, start, interval);
    }

    public static RewardTrigger total(int value) {
        return new RewardTrigger(RewardTriggerType.TOTAL, value, 0);
    }

    public static RewardTrigger allSites() {
        return new RewardTrigger(RewardTriggerType.ALL_SITES, 0, 0);
    }

    public RewardTriggerType getType() {
        return type;
    }

    /**
     * Check if this trigger should fire given the player's current stats.
     * @param totalVotes player's total votes (after increment)
     * @param streak player's current streak (after increment)
     */
    public boolean shouldFire(int totalVotes, int streak) {
        return switch (type) {
            case SINGLE -> true;
            case REPEATING -> interval > 0 && totalVotes % interval == 0;
            case STREAK -> streak == value;
            case STREAK_CYCLE -> interval > 0 && streak >= value && (streak - value) % interval == 0;
            case TOTAL -> totalVotes == value;
            case ALL_SITES -> false; // ALL_SITES requires extended context; handled separately
        };
    }

    /**
     * Check if ALL_SITES trigger should fire. This requires additional context.
     * @param data player's vote data
     * @param plugin plugin instance (for site manager access)
     * @return true if player voted all configured sites today and hasn't already claimed today
     */
    public boolean shouldFireAllSites(VotePlayerData data, me.neoblade298.ashvote.AshVote plugin) {
        // Can only claim once per calendar day
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate lastClaim = data.getLastAllSitesClaimDay();
        if (lastClaim != null && lastClaim.equals(today)) {
            return false;
        }

        // Check if player has voted on all configured sites today
        return data.hasVotedAllSitesToday(plugin);
    }
}
