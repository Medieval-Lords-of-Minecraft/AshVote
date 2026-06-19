package me.neoblade298.ashvote.leaderboard;

public class LeaderboardEntry {

    private final String playerName;
    private final int value;

    public LeaderboardEntry(String playerName, int value) {
        this.playerName = playerName;
        this.value = value;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getValue() {
        return value;
    }
}
