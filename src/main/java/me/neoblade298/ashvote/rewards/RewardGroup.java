package me.neoblade298.ashvote.rewards;

import java.util.List;

/**
 * A pure reward definition: a bundle of commands ('rewards'), a weighted
 * pick-one table ('choices'), or a permission-routed pick-one list
 * ('permissioned'). Groups never fire on their own; they only run when a
 * trigger (or another group) references them.
 */
public class RewardGroup {

    private final String id;
    private final List<String> rewards;
    private final List<WeightedChoice> choices; // empty = run all rewards; non-empty = pick one weighted
    private final List<PermissionedChoice> permissioned; // empty = not permissioned; non-empty = first authorized entry wins

    public RewardGroup(String id, List<String> rewards, List<WeightedChoice> choices,
            List<PermissionedChoice> permissioned) {
        this.id = id;
        this.rewards = rewards;
        this.choices = choices;
        this.permissioned = permissioned;
    }

    public String getId() {
        return id;
    }

    public List<String> getRewards() {
        return rewards;
    }

    public List<WeightedChoice> getChoices() {
        return choices;
    }

    public boolean hasChoices() {
        return choices != null && !choices.isEmpty();
    }

    public List<PermissionedChoice> getPermissioned() {
        return permissioned;
    }

    public boolean hasPermissioned() {
        return permissioned != null && !permissioned.isEmpty();
    }
}
