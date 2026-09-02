package me.neoblade298.ashvote.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
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
    private LocalDate lastAllSitesClaimDay; // Track last calendar day this player claimed all-sites reward
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

    public VotePlayerData(UUID uuid, int totalVotes, int monthlyVotes, int voteMonth, int streak, long lastVoteTime, int lastAllSitesClaimDayInt) {
        this.uuid = uuid;
        this.totalVotes = totalVotes;
        this.voteMonth = voteMonth;
        this.streak = streak;
        this.lastVoteTime = lastVoteTime;
        
        // Convert YYYYMMDD int to LocalDate
        if (lastAllSitesClaimDayInt > 0) {
            int year = lastAllSitesClaimDayInt / 10000;
            int month = (lastAllSitesClaimDayInt % 10000) / 100;
            int day = lastAllSitesClaimDayInt % 100;
            this.lastAllSitesClaimDay = LocalDate.of(year, month, day);
        } else {
            this.lastAllSitesClaimDay = null;
        }

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
     * Reset all vote progress while retaining zeroed persistence entries.
     */
    public void resetVotes() {
        totalVotes = 0;
        monthlyVotes = 0;
        voteMonth = currentMonth();
        streak = 0;
        lastVoteTime = 0;
        lastAllSitesClaimDay = null;
        siteCooldowns.replaceAll((siteId, lastVote) -> 0L);
        rewardClaims.replaceAll((triggerId, claims) -> 0);
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

    // All-sites reward claim tracking
    public LocalDate getLastAllSitesClaimDay() {
        return lastAllSitesClaimDay;
    }

    public void setLastAllSitesClaimDay(LocalDate day) {
        this.lastAllSitesClaimDay = day;
    }

    /**
     * Convert LocalDate to YYYYMMDD int format for database storage.
     */
    private int dateToInt(LocalDate date) {
        if (date == null) return 0;
        return date.getYear() * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
    }

    /**
     * Check if the player has voted on every configured site during that site's current calendar day.
     */
    public boolean hasVotedAllSitesToday(me.neoblade298.ashvote.AshVote plugin) {
        // Check each configured site
        var allSites = plugin.getSiteManager().getAll();
        if (allSites.isEmpty()) {
            return false; // No sites configured
        }

        for (var site : allSites) {
            long lastVote = getSiteCooldown(site.getId());
            if (!site.hasVotedToday(lastVote)) {
                return false; // Haven't voted on this site today
            }
        }
        return true; // Voted on all sites today
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
                .addValue("last_all_sites_claim_day", dateToInt(lastAllSitesClaimDay))
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
