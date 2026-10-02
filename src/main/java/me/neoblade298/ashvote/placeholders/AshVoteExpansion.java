package me.neoblade298.ashvote.placeholders;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.neoblade298.ashvote.AshVote;
import me.neoblade298.ashvote.player.PlayerManager;
import me.neoblade298.ashvote.player.VotePlayerData;

public class AshVoteExpansion extends PlaceholderExpansion {

    private final AshVote plugin;

    public AshVoteExpansion(AshVote plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ashvote";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || !params.equalsIgnoreCase("canvote")) {
            return null;
        }

        VotePlayerData data = PlayerManager.get(player);
        if (data == null) {
            return null;
        }

        return data.hasClaimedAllSitesToday() ? "Thanks for voting!" : "Vote available!";
    }
}
