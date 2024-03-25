package fr.xephi.authme.task;

import fr.euphyllia.energie.model.SchedulerCallBack;
import fr.euphyllia.energie.model.SchedulerTaskInter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Message shown to a player in a regular interval as long as he is not logged in.
 */
public class MessageTask implements SchedulerCallBack {

    private final Player player;
    private final String[] message;
    private boolean isMuted;

    /*
     * Constructor.
     */
    public MessageTask(Player player, String[] lines) {
        this.player = player;
        this.message = lines;
        isMuted = false;
    }

    public void setMuted(boolean isMuted) {
        this.isMuted = isMuted;
    }

    @Override
    public void run(@Nullable SchedulerTaskInter schedulerTask) {
        if (!isMuted) {
            player.sendMessage(message);
        }
    }
}
