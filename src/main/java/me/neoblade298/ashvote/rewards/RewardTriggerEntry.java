package me.neoblade298.ashvote.rewards;

/**
 * A configured trigger from the 'triggers' section. Pairs a firing condition
 * ('when') and gating (permission / chance / max-claims) with a reward reference
 * (a group id or a raw console command). Triggers are the only things evaluated
 * on a vote; reward groups never fire on their own.
 */
public class RewardTriggerEntry {

    private final String id;
    private final String reward; // group reference or console command
    private final RewardTrigger when;
    private final String permission;
    private final int maxClaims; // -1 = unlimited
    private final int chance; // percent [0-100] that this trigger fires when eligible

    public RewardTriggerEntry(String id, String reward, RewardTrigger when,
            String permission, int maxClaims, int chance) {
        this.id = id;
        this.reward = reward;
        this.when = when;
        this.permission = permission;
        this.maxClaims = maxClaims;
        this.chance = chance;
    }

    public String getId() {
        return id;
    }

    public String getReward() {
        return reward;
    }

    public RewardTrigger getWhen() {
        return when;
    }

    public String getPermission() {
        return permission;
    }

    public int getMaxClaims() {
        return maxClaims;
    }

    public boolean hasMaxClaims() {
        return maxClaims > 0;
    }

    public int getChance() {
        return chance;
    }

    public boolean hasChance() {
        return chance < 100;
    }
}
