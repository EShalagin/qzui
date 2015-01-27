package qzui;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import qzui.dmp.DMPBootstrap;
import restx.factory.AutoStartable;
import restx.factory.Module;
import restx.factory.Provides;

import java.io.IOException;

/**
 * Date: 18/2/14
 * Time: 21:14
 */
@Module
public class QuartzModule {
    @Provides
    public Scheduler scheduler() {
        try {
            return StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Provides
    public AutoStartable schedulerStarter(final Scheduler scheduler) {
        return new AutoStartable() {
            @Override
            public void start() {
                try {
                    scheduler.start();
                    DMPBootstrap.initializeJobsFromTasks(scheduler);
                    DMPBootstrap.subscribeToDMPEvents();
                } catch (SchedulerException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Provides
    public AutoCloseable schedulerCloser(final Scheduler scheduler) {
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                scheduler.shutdown();
            }
        };
    }
}
