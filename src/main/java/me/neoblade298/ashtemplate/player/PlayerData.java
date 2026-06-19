package me.neoblade298.ashtemplate.player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import me.neoblade298.neocore.shared.util.SQLInsertBuilder;
import me.neoblade298.neocore.shared.util.SQLInsertBuilder.SQLAction;

public class PlayerData {

    private int counter;

    /** New player with default values */
    public PlayerData() {
        this.counter = 0;
    }

    /** Load from SQL result */
    public PlayerData(ResultSet rs) throws SQLException {
        this.counter = rs.getInt("counter");
    }

    public int getCounter() {
        return counter;
    }

    public void incrementCounter() {
        counter++;
    }

    public String save(UUID uuid) {
        return new SQLInsertBuilder(SQLAction.REPLACE, "ashtemplate_data")
                .addString(uuid.toString())
                .addValue(counter)
                .build();
    }
}
