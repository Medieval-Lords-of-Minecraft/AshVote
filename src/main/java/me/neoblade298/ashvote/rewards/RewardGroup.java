package me.neoblade298.ashvote.rewards;

import java.util.List;

public class RewardGroup {

    private final String id;
    private final List<String> rewards;
    private final List<WeightedChoice> choices; // empty = run all rewards; non-empty = pick one weighted
    private final RewardTrigger trigger;
    private final String permission;
    private final int maxClaims; // -1 = unlimited
    private final int chance; // percent [0-100] that this group fires when eligible

    public RewardGroup(String id, List<String> rewards, List<WeightedChoice> choices, RewardTrigger trigger,
            String permission, int maxClaims, int chance) {
        this.id = id;
        this.rewards = rewards;
        this.choices = choices;
        this.trigger = trigger;
        this.permission = permission;
        this.maxClaims = maxClaims;
        this.chance = chance;
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

    public List<WeightedChoice> getChoices() {
        return choices;
    }

    public boolean hasChoices() {
        return choices != null && !choices.isEmpty();
    }

    public int getChance() {
        return chance;
    }

    public boolean hasChance() {
        return chance < 100;
    }
}
