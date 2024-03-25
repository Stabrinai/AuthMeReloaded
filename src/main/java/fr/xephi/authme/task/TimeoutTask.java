package fr.xephi.authme.task;

import fr.euphyllia.energie.model.SchedulerCallBack;
import fr.euphyllia.energie.model.SchedulerTaskInter;
import fr.xephi.authme.data.auth.PlayerCache;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/**
 * Kicks a player if he hasn't logged in (scheduled to run after a configured delay).
 */
public class TimeoutTask implements SchedulerCallBack {

    private final Player player;
    private final String message;
    private final PlayerCache playerCache;

    /**
     * Constructor for TimeoutTask.
     *
     * @param player the player to check
     * @param message the kick message
     * @param playerCache player cache instance
     */
    public TimeoutTask(Player player, String message, PlayerCache playerCache) {
        this.message = message;
        this.player = player;
        this.playerCache = playerCache;
    }

    @Override
    public void run(@Nullable SchedulerTaskInter schedulerTaskInter) {
        if (!playerCache.isAuthenticated(player.getName())) {
            player.kickPlayer(message);
        }
    }
}
