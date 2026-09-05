package me.neoblade298.ashvote.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.listeners.VoteListener.VoteResult;
import me.neoblade298.neocore.bukkit.NeoCore;
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
            "last_all_sites_claim_day INT NOT NULL DEFAULT 0, " +
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
        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS ashvote_votes (" +
            "id VARCHAR(36) NOT NULL, " +
            "username VARCHAR(16) NOT NULL, " +
            "service_name VARCHAR(128) NOT NULL, " +
            "received_at BIGINT NOT NULL, " +
            "state VARCHAR(16) NOT NULL DEFAULT 'PENDING', " +
            "bypass_cooldown BOOLEAN NOT NULL DEFAULT FALSE, " +
            "processing_started_at BIGINT NOT NULL DEFAULT 0, " +
            "processed_at BIGINT NOT NULL DEFAULT 0, " +
            "result_detail VARCHAR(255), " +
            "PRIMARY KEY (id))"
        );

        if (!columnExists(stmt.getConnection(), "ashvote_votes", "bypass_cooldown")) {
            stmt.executeUpdate("ALTER TABLE ashvote_votes ADD COLUMN bypass_cooldown BOOLEAN NOT NULL DEFAULT FALSE");
        }

        if (tableExists(stmt.getConnection(), "ashvote_pending_votes")) {
            stmt.executeUpdate(
                "INSERT INTO ashvote_votes (id, username, service_name, received_at, state) " +
                "SELECT pending.id, pending.username, pending.service_name, pending.received_at, 'PENDING' " +
                "FROM ashvote_pending_votes pending WHERE NOT EXISTS " +
                "(SELECT 1 FROM ashvote_votes votes WHERE votes.id = pending.id)"
            );
            stmt.executeUpdate("DROP TABLE ashvote_pending_votes");
        }
    }

    private static boolean tableExists(Connection con, String tableName) throws Exception {
        try (ResultSet tables = con.getMetaData().getTables(con.getCatalog(), null, "%", new String[] { "TABLE" })) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
            }
        }
        return false;
    }

    private static boolean columnExists(Connection con, String tableName, String columnName) throws Exception {
        try (ResultSet columns = con.getMetaData().getColumns(con.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
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
                    rs.getLong("last_vote_time"),
                    rs.getInt("last_all_sites_claim_day")
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

            List<RecordedVote> pendingVotes = new ArrayList<>();
            try (PreparedStatement pendingStmt = stmt.getConnection().prepareStatement(
                    "SELECT id, username, service_name, bypass_cooldown FROM ashvote_votes " +
                    "WHERE LOWER(username) = ? AND (state = 'PENDING' OR " +
                    "(state = 'PROCESSING' AND processing_started_at < ?)) ORDER BY received_at")) {
                pendingStmt.setString(1, p.getName().toLowerCase(Locale.ROOT));
                pendingStmt.setLong(2, System.currentTimeMillis() - 5 * 60 * 1000L);
                try (ResultSet pendingResults = pendingStmt.executeQuery()) {
                    while (pendingResults.next()) {
                        pendingVotes.add(new RecordedVote(
                                pendingResults.getString("id"),
                                pendingResults.getString("username"),
                            pendingResults.getString("service_name"),
                            pendingResults.getBoolean("bypass_cooldown")));
                    }
                }
            }

            for (RecordedVote pendingVote : pendingVotes) {
                tryProcessVote(pendingVote.id(), pendingVote.username(), pendingVote.serviceName(), pendingVote.bypassCooldown());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void tryProcessVote(String voteId, String username, String serviceName, boolean bypassCooldown) {
        long now = System.currentTimeMillis();
        try (Connection con = NeoCore.getConnection("AshVote");
             PreparedStatement stmt = con.prepareStatement(
                     "UPDATE ashvote_votes SET state = 'PROCESSING', processing_started_at = ?, result_detail = NULL " +
                     "WHERE id = ? AND (state = 'PENDING' OR (state = 'PROCESSING' AND processing_started_at < ?))")) {
            stmt.setLong(1, now);
            stmt.setString(2, voteId);
            stmt.setLong(3, now - 5 * 60 * 1000L);
            if (stmt.executeUpdate() == 0) return;
        } catch (Exception e) {
            AshVote.inst().getLogger().severe("Failed to claim recorded vote " + voteId);
            e.printStackTrace();
            return;
        }

        Bukkit.getScheduler().runTask(AshVote.inst(), () -> {
            Player player = Bukkit.getPlayerExact(username);
            VoteResult result;
            if (player == null || !player.isOnline() || data.get(player.getUniqueId()) == null) {
                result = VoteResult.retry("Player is offline or data is not loaded");
            } else {
                result = AshVote.inst().getVoteListener().processVote(player, serviceName, bypassCooldown);
            }

            updateVoteResult(voteId, result);
        });
    }

    private static void updateVoteResult(String voteId, VoteResult result) {
        Bukkit.getScheduler().runTaskAsynchronously(AshVote.inst(), () -> {
            try (Connection con = NeoCore.getConnection("AshVote");
                 PreparedStatement stmt = con.prepareStatement(
                         "UPDATE ashvote_votes SET state = ?, processed_at = ?, result_detail = ? WHERE id = ?")) {
                stmt.setString(1, result.state());
                stmt.setLong(2, "PENDING".equals(result.state()) ? 0 : System.currentTimeMillis());
                stmt.setString(3, result.detail());
                stmt.setString(4, voteId);
                stmt.executeUpdate();
            } catch (Exception e) {
                AshVote.inst().getLogger().severe("Failed to update recorded vote " + voteId + " to " + result.state());
                e.printStackTrace();
            }
        });
    }

    private record RecordedVote(String id, String username, String serviceName, boolean bypassCooldown) {
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
