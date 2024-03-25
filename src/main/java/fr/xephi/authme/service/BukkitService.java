package fr.xephi.authme.service;

import fr.euphyllia.energie.model.Scheduler;
import fr.euphyllia.energie.model.SchedulerCallBack;
import fr.euphyllia.energie.model.SchedulerTaskInter;
import fr.euphyllia.energie.model.SchedulerType;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Service for operations requiring the Bukkit API, such as for scheduling.
 */
public class BukkitService implements SettingsDependent {

    /** Number of ticks per second in the Bukkit main thread. */
    public static final int TICKS_PER_SECOND = 20;
    /** Number of ticks per minute. */
    public static final int TICKS_PER_MINUTE = 60 * TICKS_PER_SECOND;

    private final AuthMe authMe;
    private boolean useAsyncTasks;

    @Inject
    BukkitService(AuthMe authMe, Settings settings) {
        this.authMe = authMe;
        reload(settings);
    }

    /**
     * Schedules a once off task to occur as soon as possible.
     * <p>
     * This task will be executed by the main server thread.
     *
     * @param task Task to be executed
     * @return a SchedulerTaskInter
     */
    public SchedulerTaskInter scheduleSyncDelayedTask(Object object,SchedulerCallBack task) {
        return runTaskLater(object, task, 1L);
    }

    /**
     * Schedules a once off task to occur after a delay.
     * <p>
     * This task will be executed by the main server thread.
     *
     * @param task Task to be executed
     * @param delay Delay in server ticks before executing task
     * @return a SchedulerTaskInter
     */
    public SchedulerTaskInter scheduleSyncDelayedTask(Object object,SchedulerCallBack task, long delay) {
        return runTaskLater(object, task, delay);
    }

    /**
     * Schedules a synchronous task if we are currently on a async thread; if not, it runs the task immediately.
     * Use this when {@link #runTaskOptionallyAsync(SchedulerCallBack) optionally asynchronous tasks} have to
     * run something synchronously.
     *
     * @param task the task to be run
     */
    public void scheduleSyncTaskFromOptionallyAsyncTask(SchedulerCallBack task) {
        if (Bukkit.isPrimaryThread()) {
            runTask(null, task);
        } else {
            scheduleSyncDelayedTask(null, task);
        }
    }

    /**
     * Returns a task that will run on the next server tick.
     *
     * @param object judgment thread
     * @param task the task to be run
     * @return a SchedulerTaskInter
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalArgumentException if task is null
     */
    public SchedulerTaskInter runTask(Object object, SchedulerCallBack task) {
        if (object instanceof Entity) {
            return getScheduler().runTask(SchedulerType.SYNC, (Entity) object, task, null);
        } else if (object instanceof Location) {
            return getScheduler().runTask(SchedulerType.SYNC, (Location) object, task);
        } else {
            return getScheduler().runTask(SchedulerType.SYNC, task);
        }
    }

    /**
     * Returns a task that will run after the specified number of server
     * ticks.
     *
     * @param object judgment thread
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @return a SchedulerTaskInter
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalArgumentException if task is null
     */
    public SchedulerTaskInter runTaskLater(Object object, SchedulerCallBack task, long delay) {
        if (object instanceof Entity) {
            return getScheduler().runDelayed(SchedulerType.SYNC, (Entity) object, task, null, delay);
        } else if (object instanceof Location) {
            return getScheduler().runDelayed(SchedulerType.SYNC, (Location) object, task, delay);
        } else {
            return getScheduler().runDelayed(SchedulerType.SYNC, task, delay);
        }
    }
    /**
     * Schedules this task to run asynchronously or immediately executes it based on
     * AuthMe's configuration.
     *
     * @param task the task to run
     */
    public void runTaskOptionallyAsync(SchedulerCallBack task) {
        if (useAsyncTasks) {
            runTaskAsynchronously(task);
        } else {
            runTask(null, task);
        }
    }

    /**
     * <b>Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will run asynchronously.
     *
     * @param task the task to be run
     * @return a SchedulerTaskInter
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalArgumentException if task is null
     */
    public SchedulerTaskInter runTaskAsynchronously(SchedulerCallBack task) {
        return getScheduler().runTask(SchedulerType.ASYNC, task);
    }

    /**
     * <b>Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will repeatedly run asynchronously until cancelled,
     * starting after the specified number of server ticks.
     *
     * @param task the task to be run
     * @param delay the ticks to wait before running the task for the first
     *     time
     * @param period the ticks to wait between runs
     * @return a SchedulerTaskInter
     * @throws IllegalArgumentException if task is null
     * @throws IllegalStateException if this was already scheduled
     */
    public SchedulerTaskInter runTaskTimerAsynchronously(SchedulerCallBack task, long delay, long period) {
        return getScheduler().runAtFixedRate(SchedulerType.ASYNC, task, delay, period);
    }

    /**
     * Schedules the given task to repeatedly run until cancelled, starting after the
     * specified number of server ticks.
     *
     * @param object judgment thread
     * @param task the task to schedule
     * @param delay the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @return a SchedulerTaskInter
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalStateException if this was already scheduled
     */
    public SchedulerTaskInter runTaskTimer(Object object, SchedulerCallBack task, long delay, long period) {
        if (object instanceof Entity) {
            return getScheduler().runAtFixedRate(SchedulerType.SYNC, (Entity) object, task, null, delay, period);
        } else if (object instanceof Location) {
            return getScheduler().runAtFixedRate(SchedulerType.SYNC, (Location) object, task, delay, period);
        } else {
            return getScheduler().runAtFixedRate(SchedulerType.SYNC, task, delay, period);
        }
    }

    /**
     * Get the plugin's scheduler.
     *
     * @return The plugin's scheduler.
     */
    public Scheduler getScheduler() {
        return AuthMe.getEnergie().getMinecraftScheduler();
    }

    /**
     * Broadcast a message to all players.
     *
     * @param message the message
     * @return the number of players
     */
    public int broadcastMessage(String message) {
        return Bukkit.broadcastMessage(message);
    }

    /**
     * Gets the player with the exact given name, case insensitive.
     *
     * @param name Exact name of the player to retrieve
     * @return a player object if one was found, null otherwise
     */
    public Player getPlayerExact(String name) {
        return authMe.getServer().getPlayerExact(name);
    }

    /**
     * Gets the player by the given name, regardless if they are offline or
     * online.
     * <p>
     * This method may involve a blocking web request to get the UUID for the
     * given name.
     * <p>
     * This will return an object even if the player does not exist. To this
     * method, all players will exist.
     *
     * @param name the name the player to retrieve
     * @return an offline player
     */
    public OfflinePlayer getOfflinePlayer(String name) {
        return authMe.getServer().getOfflinePlayer(name);
    }

    /**
     * Gets a set containing all banned players.
     *
     * @return a set containing banned players
     */
    public Set<OfflinePlayer> getBannedPlayers() {
        return Bukkit.getBannedPlayers();
    }

    /**
     * Gets every player that has ever played on this server.
     *
     * @return an array containing all previous players
     */
    public OfflinePlayer[] getOfflinePlayers() {
        return Bukkit.getOfflinePlayers();
    }

    /**
     * Gets a view of all currently online players.
     *
     * @return collection of online players
     */
    @SuppressWarnings("unchecked")
    public Collection<Player> getOnlinePlayers() {
        return (Collection<Player>) Bukkit.getOnlinePlayers();
    }

    /**
     * Calls an event with the given details.
     *
     * @param event Event details
     * @throws IllegalStateException Thrown when an asynchronous event is
     *     fired from synchronous code.
     */
    public void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Creates an event with the provided function and emits it.
     *
     * @param eventSupplier the event supplier: function taking a boolean specifying whether AuthMe is configured
     *                      in async mode or not
     * @param <E> the event type
     * @return the event that was created and emitted
     */
    public <E extends Event> E createAndCallEvent(Function<Boolean, E> eventSupplier) {
        E event = eventSupplier.apply(useAsyncTasks);
        callEvent(event);
        return event;
    }

    /**
     * Gets the world with the given name.
     *
     * @param name the name of the world to retrieve
     * @return a world with the given name, or null if none exists
     */
    public World getWorld(String name) {
        return Bukkit.getWorld(name);
    }

    /**
     * Dispatches a command on this server, and executes it if found.
     *
     * @param sender the apparent sender of the command
     * @param commandLine the command + arguments. Example: <code>test abc 123</code>
     * @return returns false if no target is found
     */
    public boolean dispatchCommand(CommandSender sender, String commandLine) {
        if (sender == null) return false;
        runTask(sender, task -> Bukkit.dispatchCommand(sender, commandLine));
        return true;
    }

    /**
     * Dispatches a command to be run as console user on this server, and executes it if found.
     *
     * @param commandLine the command + arguments. Example: <code>test abc 123</code>
     * @return returns false if no target is found
     */
    public boolean dispatchConsoleCommand(String commandLine) {
        runTask(null, task -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine));
        return true;
    }

    @Override
    public void reload(Settings settings) {
        useAsyncTasks = settings.getProperty(PluginSettings.USE_ASYNC_TASKS);
    }

    /**
     * Send the specified bytes to bungeecord using the specified player connection.
     *
     * @param player the player
     * @param bytes the message
     */
    public void sendBungeeMessage(Player player, byte[] bytes) {
        player.sendPluginMessage(authMe, "BungeeCord", bytes);
    }

    /**
     * Adds a ban to the list. If a previous ban exists, this will
     * update the previous entry.
     *
     * @param ip the ip of the ban
     * @param reason reason for the ban, null indicates implementation default
     * @param expires date for the ban's expiration (unban), or null to imply
     *     forever
     * @param source source of the ban, null indicates implementation default
     * @return the entry for the newly created ban, or the entry for the
     *     (updated) previous ban
     */
    public BanEntry banIp(String ip, String reason, Date expires, String source) {
        return Bukkit.getServer().getBanList(BanList.Type.IP).addBan(ip, reason, expires, source);
    }

    /**
     * Returns an optional with a boolean indicating whether bungeecord is enabled or not if the
     * server implementation is Spigot. Otherwise returns an empty optional.
     *
     * @return Optional with configuration value for Spigot, empty optional otherwise
     */
    public Optional<Boolean> isBungeeCordConfiguredForSpigot() {
        try {
            YamlConfiguration spigotConfig = Bukkit.spigot().getConfig();
            return Optional.of(spigotConfig.getBoolean("settings.bungeecord"));
        } catch (NoSuchMethodError e) {
            return Optional.empty();
        }
    }

    /**
     * @return the IP string that this server is bound to, otherwise empty string
     */
    public String getIp() {
        return Bukkit.getServer().getIp();
    }
}
