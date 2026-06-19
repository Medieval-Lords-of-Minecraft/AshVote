package me.neoblade298.ashvote.leaderboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import me.neoblade298.neocore.bukkit.NeoCore;

public class LeaderboardManager {

    private final JavaPlugin plugin;
    private List<LeaderboardEntry> topMonthly = new ArrayList<>();
    private List<LeaderboardEntry> topAllTime = new ArrayList<>();
    private List<LeaderboardEntry> topStreaks = new ArrayList<>();

    public LeaderboardManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void refreshAll() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            topMonthly = queryTop("monthly_votes", true);
            topAllTime = queryTop("total_votes", false);
            topStreaks = queryTop("streak", false);
        });
    }

    public List<LeaderboardEntry> getTopMonthly() {
        return topMonthly;
    }

    public List<LeaderboardEntry> getTopAllTime() {
        return topAllTime;
    }

    public List<LeaderboardEntry> getTopStreaks() {
        return topStreaks;
    }

    private List<LeaderboardEntry> queryTop(String column, boolean monthFilter) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (Connection con = NeoCore.getConnection("AshVote")) {
            String sql;
            if (monthFilter) {
                int currentMonth = YearMonth.now().getYear() * 100 + YearMonth.now().getMonthValue();
                sql = "SELECT uuid, " + column + " FROM ashvote_players WHERE vote_month = " + currentMonth +
                      " ORDER BY " + column + " DESC LIMIT 10";
            } else {
                sql = "SELECT uuid, " + column + " FROM ashvote_players ORDER BY " + column + " DESC LIMIT 10";
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    int value = rs.getInt(column);
                    String name = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();
                    if (name == null) name = uuid.substring(0, 8);
                    entries.add(new LeaderboardEntry(name, value));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to query leaderboard for " + column + ": " + e.getMessage());
        }
        return entries;
    }
}
