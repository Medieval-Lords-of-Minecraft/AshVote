package me.neoblade298.ashvote.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.neoblade298.neocore.bukkit.io.IOComponent;

public class PlayerManager implements IOComponent {

    private static final HashMap<UUID, VotePlayerData> data = new HashMap<>();

    public static VotePlayerData get(Player p) {
        return data.get(p.getUniqueId());
    }

    public static VotePlayerData get(UUID uuid) {
        return data.get(uuid);
    }

    public static void initTables(Statement stmt) throws Exception {
        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS ashvote_players (" +
            "uuid VARCHAR(36) NOT NULL, " +
            "total_votes INT NOT NULL DEFAULT 0, " +
            "monthly_votes INT NOT NULL DEFAULT 0, " +
            "vote_month INT NOT NULL DEFAULT 0, " +
            "streak INT NOT NULL DEFAULT 0, " +
            "last_vote_time BIGINT NOT NULL DEFAULT 0, " +
            "PRIMARY KEY (uuid))"
        );
        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS ashvote_site_history (" +
            "uuid VARCHAR(36) NOT NULL, " +
            "site_id VARCHAR(64) NOT NULL, " +
            "last_vote_time BIGINT NOT NULL DEFAULT 0, " +
            "PRIMARY KEY (uuid, site_id))"
        );
        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS ashvote_reward_claims (" +
            "uuid VARCHAR(36) NOT NULL, " +
            "reward_group VARCHAR(64) NOT NULL, " +
            "times_claimed INT NOT NULL DEFAULT 0, " +
            "PRIMARY KEY (uuid, reward_group))"
        );
    }

    @Override
    public void preloadPlayer(OfflinePlayer p, Statement stmt) {
    }

    @Override
    public void loadPlayer(Player p, Statement stmt) {
        UUID uuid = p.getUniqueId();
        try {
            VotePlayerData pd;
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM ashvote_players WHERE uuid = '" + uuid + "'"
            );
            if (rs.next()) {
                pd = new VotePlayerData(uuid,
                    rs.getInt("total_votes"),
                    rs.getInt("monthly_votes"),
                    rs.getInt("vote_month"),
                    rs.getInt("streak"),
                    rs.getLong("last_vote_time")
                );
            } else {
                pd = new VotePlayerData(uuid);
            }

            // Load site cooldowns
            rs = stmt.executeQuery(
                "SELECT site_id, last_vote_time FROM ashvote_site_history WHERE uuid = '" + uuid + "'"
            );
            while (rs.next()) {
                pd.setSiteCooldown(rs.getString("site_id"), rs.getLong("last_vote_time"));
            }

            // Load reward claims
            rs = stmt.executeQuery(
                "SELECT reward_group, times_claimed FROM ashvote_reward_claims WHERE uuid = '" + uuid + "'"
            );
            while (rs.next()) {
                pd.getRewardClaims().put(rs.getString("reward_group"), rs.getInt("times_claimed"));
            }

            data.put(uuid, pd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void savePlayer(Player p, Connection con, List<PreparedStatement> stmts) throws Exception {
        UUID uuid = p.getUniqueId();
        VotePlayerData pd = data.get(uuid);
        if (pd == null) return;

        stmts.add(pd.savePlayer(con));

        PreparedStatement sitesStmt = pd.saveSiteCooldowns(con);
        if (sitesStmt != null) stmts.add(sitesStmt);

        PreparedStatement claimsStmt = pd.saveRewardClaims(con);
        if (claimsStmt != null) stmts.add(claimsStmt);
    }

    @Override
    public void cleanup(Connection con, List<PreparedStatement> stmts) throws Exception {
        data.clear();
    }
}
