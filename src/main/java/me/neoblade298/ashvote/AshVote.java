package me.neoblade298.ashvote;

import java.sql.Connection;
import java.sql.Statement;

import org.bukkit.plugin.java.JavaPlugin;

import me.neoblade298.ashvote.commands.CmdVote;
import me.neoblade298.ashvote.commands.CmdVoteAdminFakevote;
import me.neoblade298.ashvote.commands.CmdVoteAdminGivereward;
import me.neoblade298.ashvote.commands.CmdVoteAdminReload;
import me.neoblade298.ashvote.commands.CmdVoteAdminSet;
import me.neoblade298.ashvote.commands.CmdVoteLeaderboard;
import me.neoblade298.ashvote.commands.CmdVoteStats;
import me.neoblade298.ashvote.config.ConfigManager;
import me.neoblade298.ashvote.leaderboard.LeaderboardManager;
import me.neoblade298.ashvote.listeners.VoteListener;
import me.neoblade298.ashvote.player.PlayerManager;
import me.neoblade298.ashvote.rewards.RewardManager;
import me.neoblade298.ashvote.sites.SiteManager;
import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.commands.SubcommandManager;
import net.kyori.adventure.text.format.NamedTextColor;

public class AshVote extends JavaPlugin {

    private static AshVote inst;

    private ConfigManager configManager;
    private SiteManager siteManager;
    private RewardManager rewardManager;
    private PlayerManager playerManager;
    private LeaderboardManager leaderboardManager;
    private VoteListener voteListener;

    public static AshVote inst() {
        return inst;
    }

    @Override
    public void onEnable() {
        inst = this;

        siteManager = new SiteManager();
        rewardManager = new RewardManager();
        configManager = new ConfigManager(this);
        playerManager = new PlayerManager();
        leaderboardManager = new LeaderboardManager(this);
        voteListener = new VoteListener(this);

        // Load configs
        configManager.reload(siteManager, rewardManager);

        // Create tables
        try (Connection con = NeoCore.getConnection("AshVote");
             Statement stmt = con.createStatement()) {
            PlayerManager.initTables(stmt);
        } catch (Exception e) {
            getLogger().severe("Failed to create AshVote tables!");
            e.printStackTrace();
        }

        // Register IO component
        NeoCore.registerIOComponent(this, playerManager, "AshVote-PlayerManager");

        // Register listener
        getServer().getPluginManager().registerEvents(voteListener, this);

        // Register commands
        initCommands();

        // Cache leaderboards
        leaderboardManager.refreshAll();

        getLogger().info("AshVote enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AshVote disabled!");
    }

    private void initCommands() {
        // /vote command
        SubcommandManager voteCmds = new SubcommandManager("vote", "ashvote.vote", NamedTextColor.GOLD, this);
        CmdVote cmdVote = new CmdVote();
        cmdVote.enableTabComplete();
        voteCmds.register(cmdVote);

        CmdVoteStats cmdStats = new CmdVoteStats();
        cmdStats.overrideTabHandler();
        voteCmds.register(cmdStats);

        CmdVoteLeaderboard cmdLb = new CmdVoteLeaderboard();
        cmdLb.enableTabComplete();
        voteCmds.register(cmdLb);

        // /voteadmin command
        SubcommandManager adminCmds = new SubcommandManager("voteadmin", "ashvote.admin", NamedTextColor.RED, this);

        CmdVoteAdminSet cmdSet = new CmdVoteAdminSet();
        cmdSet.overrideTabHandler();
        adminCmds.register(cmdSet);

        CmdVoteAdminFakevote cmdFake = new CmdVoteAdminFakevote();
        cmdFake.overrideTabHandler();
        adminCmds.register(cmdFake);

        CmdVoteAdminReload cmdReload = new CmdVoteAdminReload();
        cmdReload.enableTabComplete();
        adminCmds.register(cmdReload);

        CmdVoteAdminGivereward cmdGive = new CmdVoteAdminGivereward();
        cmdGive.overrideTabHandler();
        adminCmds.register(cmdGive);
    }

    public void reloadConfigs() {
        configManager.reload(siteManager, rewardManager);
        leaderboardManager.refreshAll();
    }

    public SiteManager getSiteManager() {
        return siteManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public VoteListener getVoteListener() {
        return voteListener;
    }
}
