package com.github.qvp.stats;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.qvp.LocustConfig;
import com.github.qvp.env.Environment;
import com.github.qvp.event.Events;
import com.github.qvp.runners.MasterRunner;
import com.github.qvp.runners.Runner.State;

/**
 * @date 2020-07-17
 * @author zhangyuqiang02@playcrab.com
 */
public enum StatsService {
    /**
     *
     */
    INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger("STATS");

    private Thread thread;

    private volatile boolean running = false;
    private volatile boolean shutdown = false;

    private Environment environment;

    private volatile boolean print = true;

    public void start(Environment environment) {
        this.environment = environment;

        thread = new Thread(() -> {
            statsPrinter();
        });
        thread.setName("Robot-StatsEngine");
        thread.start();
    }

    public void offPrint() {
        print = false;
    }

    private static final int taskRate = (int) (LocustConfig.CONSOLE_STATS_INTERVAL_SEC * (0.9d));


    public synchronized void statsPrinter() {
        if (running) {
            return;
        }

        long start, end, delay = 2000 - taskRate;
        running = true;
        while (running) {
            start = Instant.now().toEpochMilli();

            long l = runAllTasks(start + taskRate);

            if (l < 0L) {
                l = 0L;
            }

            if (print) {
                printStats(true);
            }

            try {
                Thread.sleep(delay + l);
            } catch (InterruptedException e) {
                //pass
            }
        }

        finishPrint();

        shutdown = true;
    }

    private long runAllTasks(final long deadline) {
        int runTasks = 0;
        Runnable r;
        while ((r = runnables.poll()) != null) {
            try {
                r.run();
            } catch (Exception e) {
                e.printStackTrace();
            }

            runTasks++;

            if (deadline > 0) {
                if ((runTasks & 0x3F) == 0) {
                    long now = Instant.now().toEpochMilli();
                    if (now >= deadline) {
                        return 0L;
                    }
                }
            }
        }

        return deadline - Instant.now().toEpochMilli();
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        thread.interrupt();

        while (!shutdown) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        logger.info("stats engine stopped");
    }

    public void finishPrint() {
        runAllTasks(0);

        if (print) {
            printStats(false);
            printPercentileStats();
            printErrorReport();
        }
    }

    private ConcurrentLinkedQueue<Runnable> runnables = new ConcurrentLinkedQueue<>();


    public void printStats(boolean current) {
        RequestStats globalStats = environment.stats;
        TreeMap<String, StatsEntry> stats = globalStats.getEntries();
        if (stats.isEmpty()) {
            return;
        }

        logger.info(
                "|          Name|       reqs|            fails|       Avg|       Min|       Max|     < Median|  req/s (10)|  failures/s|");
        logger.info(
                "+--------------+-----------+-----------------+----------+----------+----------+-------------+------------+------------+");

        Iterator<Entry<String, StatsEntry>> it = stats.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, StatsEntry> entryEntry = it.next();
            StatsEntry r = entryEntry.getValue();

            logger.info("{}", r.to_string(current));
        }

        logger.info(
                "+--------------+-----------+-----------------+----------+----------+----------+-------------+------------+------------+");

        logger.info("{}", globalStats.getTotal().to_string(current));
        logger.info("");
    }

    public void printPercentileStats() {
        RequestStats globalStats = environment.stats;
        TreeMap<String, StatsEntry> stats = globalStats.getEntries();
        if (stats.isEmpty()) {
            return;
        }

        logger.info("Percentage of the requests completed within given times");
        logger.info(
                "|          Name|       reqs|      <50%|      <66%|      <75%|      <80%|      <90%|      <95%|      <98%|      <99%|      <100%|");
        logger.info(
                "+--------------+-----------+----------+----------+----------+----------+----------+----------+----------+----------+-----------+");


        for (Entry<String, StatsEntry> entryEntry : stats.entrySet()) {
            StatsEntry r = entryEntry.getValue();

            if (!r.getResponseTimes().isEmpty()) {
                logger.info(r.percentile());
            }
        }

        logger.info(
                "+--------------+-----------+----------+----------+----------+----------+----------+----------+----------+----------+-----------+");

        StatsEntry totalStats = globalStats.getTotal();
        if (!totalStats.getResponseTimes().isEmpty()) {
            logger.info(totalStats.percentile());
        }
        logger.info("");
    }

    public void printErrorReport() {
        RequestStats globalStats = environment.stats;

        Map<String, StatsError> errors = globalStats.getErrors();
        if (errors.isEmpty()) {
            return;
        }

        logger.info("Error report");
        logger.info("|      occurrences|                      Error|");
        logger.info("+-----------------+---------------------------+");
        for (StatsError error : errors.values()) {
            logger.info("{}", String.format("|%17d|%27s|", error.getOccurrences(), error.toName()));
        }
        logger.info("+-----------------+---------------------------+");
        logger.info("");
    }

    public void stats_history(MasterRunner runner) {
        if (!LocustConfig.use_response_times_cache) {
            return;
        }

        while (true) {
            if (runner.getState() != State.STATE_STOPPED) {

            }
        }
    }


    public void setup_distributed_stats_event_listeners(Events events, RequestStats stats) {
        events.worker_report.add_listener((event) -> {
            String cid = event.getArg();
            Map<String, Object> data = event.getArg();
            onWorkerReport(stats, data);
        });
    }

    private void onWorkerReport(RequestStats stats, Map<String, Object> data) {
        runnables.add(() -> {
            List<Map<String, Object>> stats_data = (List<Map<String, Object>>) data.get("stats");
            for (Map<String, Object> statsData : stats_data) {
                StatsEntry entry = StatsEntry.unserialize(statsData);
                String requestKey = entry.getName() + entry.getMethod();
                StatsEntry had = stats.getEntries().get(requestKey);
                if (had == null) {
                    String key = entry.getName() + entry.getMethod();
                    had = new StatsEntry(stats, entry.getName(), entry.getMethod(), true);
                    stats.getEntries().put(key, had);
                }

                had.extend(entry);
            }

            Map<String, Map<String, Object>> otherErrors = (Map<String, Map<String, Object>>) data
                    .get("errors");
            for (Map.Entry<String, Map<String, Object>> entry : otherErrors.entrySet()) {
                String key = entry.getKey();
                StatsError error = stats.getErrors().get(key);
                if (error == null) {
                    error = StatsError.from_dict(entry.getValue());
                    stats.getErrors().put(key, error);
                } else {
                    error.extend((long) entry.getValue().get("occurrences"));
                }
            }

            Map<String, Object> totals = (Map<String, Object>) data.get("stats_total");
            stats.getTotal().extend(StatsEntry.unserialize(totals));
        });
    }
}
