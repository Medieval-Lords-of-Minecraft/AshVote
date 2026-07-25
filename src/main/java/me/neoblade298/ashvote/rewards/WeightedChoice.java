package me.neoblade298.ashvote.rewards;

public class WeightedChoice {

    private final int weight;
    private final String reward;

    public WeightedChoice(int weight, String reward) {
        this.weight = weight;
        this.reward = reward;
    }

    public int getWeight() {
        return weight;
    }

    public String getReward() {
        return reward;
    }
}
