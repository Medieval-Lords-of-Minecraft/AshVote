package me.neoblade298.ashvote.rewards;

public class PermissionedChoice {

    private final String permission; // null = default (no permission required)
    private final String reward;

    public PermissionedChoice(String permission, String reward) {
        this.permission = permission;
        this.reward = reward;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isDefault() {
        return permission == null;
    }

    public String getReward() {
        return reward;
    }
}
