package me.neoblade298.ashtemplate.player;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.neoblade298.neocore.bukkit.io.IOComponent;

public class PlayerManager implements IOComponent {

    private static final HashMap<UUID, PlayerData> data = new HashMap<>();

    public static PlayerData get(Player p) {
        return data.get(p.getUniqueId());
    }

    @Override
    public void preloadPlayer(OfflinePlayer p, Statement stmt) {
        try {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS ashtemplate_data (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "counter INT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (uuid))"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadPlayer(Player p, Statement stmt) {
        UUID uuid = p.getUniqueId();
        try {
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM ashtemplate_data WHERE uuid = '" + uuid + "'"
            );

            if (rs.next()) {
                data.put(uuid, new PlayerData(rs));
            } else {
                data.put(uuid, new PlayerData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void savePlayer(Player p, Statement insert, Statement delete) {
        UUID uuid = p.getUniqueId();
        PlayerData pd = data.get(uuid);
        if (pd != null) {
            try {
                insert.executeUpdate(pd.save(uuid));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cleanup(Statement insert, Statement delete) {
        data.clear();
    }
}
