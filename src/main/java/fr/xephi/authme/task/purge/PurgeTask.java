package fr.xephi.authme.task.purge;

import fr.euphyllia.energie.model.SchedulerCallBack;
import fr.euphyllia.energie.model.SchedulerTaskInter;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

class PurgeTask implements SchedulerCallBack {

    //how many players we should check for each tick
    private static final int INTERVAL_CHECK = 5;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(PurgeTask.class);
    private final PurgeService purgeService;
    private final SchedulerTaskInter taskInter;

    private final PermissionsManager permissionsManager;
    private final UUID sender;
    private final Set<String> toPurge;

    private final OfflinePlayer[] offlinePlayers;
    private final int totalPurgeCount;

    private int currentPage = 0;

    /**
     * Constructor.
     *
     * @param service the purge service
     * @param permissionsManager the permissions manager
     * @param sender the sender who initiated the purge, or null
     * @param toPurge lowercase names to purge
     * @param offlinePlayers offline players to map to the names
     */
    PurgeTask(PurgeService service, PermissionsManager permissionsManager, CommandSender sender,
              Set<String> toPurge, OfflinePlayer[] offlinePlayers, SchedulerTaskInter inter) {
        this.purgeService = service;
        this.permissionsManager = permissionsManager;

        if (sender instanceof Player) {
            this.sender = ((Player) sender).getUniqueId();
        } else {
            this.sender = null;
        }

        this.toPurge = toPurge;
        this.totalPurgeCount = toPurge.size();
        this.offlinePlayers = offlinePlayers;
        this.taskInter = inter;
    }

    @Override
    public void run(SchedulerTaskInter schedulerTaskInter) {
        if (toPurge.isEmpty()) {
            //everything was removed
            finish();
            return;
        }

        Set<OfflinePlayer> playerPortion = new HashSet<>(INTERVAL_CHECK);
        Set<String> namePortion = new HashSet<>(INTERVAL_CHECK);
        for (int i = 0; i < INTERVAL_CHECK; i++) {
            int nextPosition = (currentPage * INTERVAL_CHECK) + i;
            if (offlinePlayers.length <= nextPosition) {
                //no more offline players on this page
                break;
            }

            OfflinePlayer offlinePlayer = offlinePlayers[nextPosition];
            if (offlinePlayer.getName() != null && toPurge.remove(offlinePlayer.getName().toLowerCase(Locale.ROOT))) {
                if (!permissionsManager.loadUserData(offlinePlayer)) {
                    logger.warning("Unable to check if the user " + offlinePlayer.getName() + " can be purged!");
                    continue;
                }
                if (!permissionsManager.hasPermissionOffline(offlinePlayer, PlayerStatePermission.BYPASS_PURGE)) {
                    playerPortion.add(offlinePlayer);
                    namePortion.add(offlinePlayer.getName());
                }
            }
        }

        if (!toPurge.isEmpty() && playerPortion.isEmpty()) {
            logger.info("Finished lookup of offlinePlayers. Begin looking purging player names only");

            //we went through all offlineplayers but there are still names remaining
            for (String name : toPurge) {
                if (!permissionsManager.hasPermissionOffline(name, PlayerStatePermission.BYPASS_PURGE)) {
                    namePortion.add(name);
                }
            }
            toPurge.clear();
        }

        currentPage++;
        purgeService.executePurge(playerPortion, namePortion);
        if (currentPage % 20 == 0) {
            int completed = totalPurgeCount - toPurge.size();
            sendMessage("[AuthMe] Purge progress " + completed + '/' + totalPurgeCount);
        }
    }

    private void finish() {
        taskInter.cancel();

        // Show a status message
        sendMessage(ChatColor.GREEN + "[AuthMe] Database has been purged successfully");

        logger.info("Purge finished!");
        purgeService.setPurging(false);
    }

    private void sendMessage(String message) {
        if (sender == null) {
            Bukkit.getConsoleSender().sendMessage(message);
        } else {
            Player player = Bukkit.getPlayer(sender);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
}
