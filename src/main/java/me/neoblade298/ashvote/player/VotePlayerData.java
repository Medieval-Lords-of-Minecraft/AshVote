package me.neoblade298.ashvote.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.neoblade298.neocore.shared.util.SQLInsertBuilder;
import me.neoblade298.neocore.shared.util.SQLInsertBuilder.SQLAction;

public class VotePlayerData {

    private final UUID uuid;
    private int totalVotes;
    private int monthlyVotes;
    private int voteMonth; // YYYYMM format
    private int streak;
    private long lastVoteTime;
    private final Map<String, Long> siteCooldowns = new HashMap<>();
    private final Map<String, Integer> rewardClaims = new HashMap<>();

    public VotePlayerData(UUID uuid) {
        this.uuid = uuid;
        this.totalVotes = 0;
        this.monthlyVotes = 0;
        this.voteMonth = currentMonth();
        this.streak = 0;
        this.lastVoteTime = 0;
    }

    public VotePlayerData(UUID uuid, int totalVotes, int monthlyVotes, int voteMonth, int streak, long lastVoteTime) {
        this.uuid = uuid;
        this.totalVotes = totalVotes;
        this.voteMonth = voteMonth;
        this.streak = streak;
        this.lastVoteTime = lastVoteTime;

        // Reset monthly if month has changed
        if (voteMonth != currentMonth()) {
            this.monthlyVotes = 0;
            this.voteMonth = currentMonth();
        } else {
            this.monthlyVotes = monthlyVotes;
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public int getMonthlyVotes() {
        return monthlyVotes;
    }

    public int getStreak() {
        return streak;
    }

    public long getLastVoteTime() {
        return lastVoteTime;
    }

    public void setTotalVotes(int totalVotes) {
        this.totalVotes = totalVotes;
    }

    public void setMonthlyVotes(int monthlyVotes) {
        this.monthlyVotes = monthlyVotes;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    /**
     * Process a vote: update streak, increment counters, record time.
     */
    public void recordVote() {
        long now = System.currentTimeMillis();

        // Reset streak if more than 48 hours since last vote
        if (lastVoteTime > 0 && (now - lastVoteTime) > 48 * 60 * 60 * 1000L) {
            streak = 0;
        }

        // Reset monthly if month rolled over
        if (voteMonth != currentMonth()) {
            monthlyVotes = 0;
            voteMonth = currentMonth();
        }

        totalVotes++;
        monthlyVotes++;
        streak++;
        lastVoteTime = now;
    }

    // Site cooldowns
    public long getSiteCooldown(String siteId) {
        return siteCooldowns.getOrDefault(siteId, 0L);
    }

    public void setSiteCooldown(String siteId, long time) {
        siteCooldowns.put(siteId, time);
    }

    public Map<String, Long> getSiteCooldowns() {
        return siteCooldowns;
    }

    // Reward claims
    public int getClaimCount(String groupId) {
        return rewardClaims.getOrDefault(groupId, 0);
    }

    @SuppressWarnings("null")
    public void incrementClaimCount(String groupId) {
        rewardClaims.merge(groupId, 1, Integer::sum);
    }

    public Map<String, Integer> getRewardClaims() {
        return rewardClaims;
    }

    // SQL save methods
    public PreparedStatement savePlayer(Connection con) throws SQLException {
        return new SQLInsertBuilder(SQLAction.REPLACE, "ashvote_players")
                .addValue("uuid", uuid.toString())
                .addValue("total_votes", totalVotes)
                .addValue("monthly_votes", monthlyVotes)
                .addValue("vote_month", voteMonth)
                .addValue("streak", streak)
                .addValue("last_vote_time", lastVoteTime)
                .addRow()
                .build(con);
    }

    public PreparedStatement saveSiteCooldowns(Connection con) throws SQLException {
        SQLInsertBuilder builder = new SQLInsertBuilder(SQLAction.REPLACE, "ashvote_site_history");
        for (Map.Entry<String, Long> entry : siteCooldowns.entrySet()) {
            builder.addValue("uuid", uuid.toString())
                   .addValue("site_id", entry.getKey())
                   .addValue("last_vote_time", entry.getValue())
                   .addRow();
        }
        if (siteCooldowns.isEmpty()) return null;
        return builder.build(con);
    }

    public PreparedStatement saveRewardClaims(Connection con) throws SQLException {
        SQLInsertBuilder builder = new SQLInsertBuilder(SQLAction.REPLACE, "ashvote_reward_claims");
        for (Map.Entry<String, Integer> entry : rewardClaims.entrySet()) {
            builder.addValue("uuid", uuid.toString())
                   .addValue("reward_group", entry.getKey())
                   .addValue("times_claimed", entry.getValue())
                   .addRow();
        }
        if (rewardClaims.isEmpty()) return null;
        return builder.build(con);
    }

    private static int currentMonth() {
        YearMonth ym = YearMonth.now();
        return ym.getYear() * 100 + ym.getMonthValue();
    }
}
