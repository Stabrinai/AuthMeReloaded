package fr.xephi.authme.initialization;

import fr.euphyllia.energie.model.Scheduler;
import fr.euphyllia.energie.model.SchedulerCallBack;
import fr.euphyllia.energie.model.SchedulerTaskInter;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSource;

/**
 * Waits for asynchronous tasks to complete before closing the data source
 * so the plugin can shut down properly.
 */
public class TaskCloser implements SchedulerCallBack {

    private final Scheduler scheduler;

    private final DataSource dataSource;

    /**
     * Constructor.
     *
     * @param dataSource the data source (nullable)
     */
    public TaskCloser(DataSource dataSource) {
        this.scheduler = AuthMe.getEnergie().getMinecraftScheduler();
        this.dataSource = dataSource;
    }

    @Override
    public void run(SchedulerTaskInter schedulerTaskInter) {
        scheduler.cancelAllTask();
        if (dataSource != null) {
            dataSource.closeConnection();
        }
    }
}
