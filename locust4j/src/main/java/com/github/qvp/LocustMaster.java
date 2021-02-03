package com.github.qvp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.qvp.env.Environment;
import com.github.qvp.event.ArgEvent;
import com.github.qvp.runners.MasterRunner;
import com.github.qvp.stats.StatsService;

/**
 * Locust class exposes all the APIs of locust4j.
 * Use Locust.getInstance() to get a Locust singleton.
 *
 * @author myzhan
 */
public class LocustMaster {

    private static final Logger logger = LoggerFactory.getLogger(LocustMaster.class);

    private volatile boolean started = false;
    private MasterRunner runner;

    private LocustMaster() {

    }

    private static class InstanceHolder {
        private static final LocustMaster LOCUST = new LocustMaster();
    }

    /**
     * Get the locust singleton.
     *
     * @return a Locust singleton
     * @since 1.0.0
     */
    public static LocustMaster getInstance() {
        return InstanceHolder.LOCUST;
    }


    /**
     * Add tasks to Runner, connect to master and wait for messages of master.
     *
     * @since 1.0.0
     */
    public synchronized void run() {
        if (LocustConfig.WorkerCount <= 0) {
            throw new IllegalArgumentException("need set slave count");
        }

        if (this.started) {
            return;
        }

        Environment environment = new Environment();

        MasterRunner runner = environment
                .create_master_runner(LocustConfig.MasterHost, LocustConfig.MasterPort);

        int i = 0;
        int readyCount = runner.readyClientCount();
        while (readyCount < LocustConfig.WorkerCount) {
            if (i % 2 == 0) {
                logger.info("Waiting for worker to be ready, {} of {} connected", readyCount,
                        LocustConfig.WorkerCount);
            }
            i++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            readyCount = runner.readyClientCount();
        }

        StatsService.INSTANCE.start(environment);

        runner.startSpawning(LocustConfig.UserCount, LocustConfig.SpawnRate);

        addShutdownHook(environment);

        this.started = true;
    }

    /**
     * Stop locust
     *
     * @since 1.0.7
     */
    public synchronized void shutdown(Environment environment) {
        if (!started) {
            return;
        }

        logger.info("Running teardowns...");

        this.started = false;

        environment.events.quitting.fire(ArgEvent.build(environment), true);


        if (environment.runner != null) {
            environment.runner.quit();
        }

        StatsService.INSTANCE.stop();

        // print_stats(runner.stats, current=False)
        // print_percentile_stats(runner.stats)
        // print_error_report(runner.stats)
    }

    /**
     * when JVM is shutting down, send a quit message to master, then master will remove this slave from its list.
     */
    private void addShutdownHook(Environment environment) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LocustMaster.getInstance().shutdown(environment);
            }
        });
    }
}
