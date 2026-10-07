package me.neoblade298.ashvote.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import me.neoblade298.ashvote.AshVote;

public class RewardDeliveryListener implements Listener {

    private final AshVote plugin;

    public RewardDeliveryListener(AshVote plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.getRewardManager().deliverPendingRewards(event.getPlayer());
    }
}
