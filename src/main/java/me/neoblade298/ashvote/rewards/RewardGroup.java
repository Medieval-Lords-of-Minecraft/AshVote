package me.neoblade298.ashvote.rewards;

import java.util.List;

public class RewardGroup {

    private final String id;
    private final List<String> rewards;
    private final RewardTrigger trigger;
    private final String permission;
    private final int maxClaims; // -1 = unlimited

    public RewardGroup(String id, List<String> rewards, RewardTrigger trigger, String permission, int maxClaims) {
        this.id = id;
        this.rewards = rewards;
        this.trigger = trigger;
        this.permission = permission;
        this.maxClaims = maxClaims;
    }

    public String getId() {
        return id;
    }

    public List<String> getRewards() {
        return rewards;
    }

    public RewardTrigger getTrigger() {
        return trigger;
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
}
