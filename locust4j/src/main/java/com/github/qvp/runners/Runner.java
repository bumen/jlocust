package com.github.qvp.runners;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.qvp.runners.Runner.State.*;

import com.github.qvp.env.Environment;
import com.github.qvp.LocustConfig;
import com.github.qvp.stats.RequestStats;
import com.github.qvp.stats.TracebackError;
import com.sun.management.OperatingSystemMXBean;

/**
 * @date 2021-02-01
 * @author zhangyuqiang02@playcrab.com
 */
public abstract class Runner {

    protected static final Logger logger = LoggerFactory.getLogger(Runner.class);

    protected volatile State state;

    private volatile double current_cpu_usage;

    private volatile boolean cpu_warning_emitted;

    protected int target_user_count;

    private boolean connection_broken;

    private Map<String, TracebackError> exceptions;

    protected Environment environment;

    protected RequestStats stats;

    protected ScheduledExecutorService executor;

    public Runner(Environment environment) {
        this.environment = environment;
        this.state = STATE_INIT;
        this.current_cpu_usage = 0;
        this.cpu_warning_emitted = false;
        this.exceptions= new HashMap<>();
        this.target_user_count = 0;
        this.connection_broken = false;

        this.stats = environment.stats;


        this.executor = Executors.newScheduledThreadPool(3, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                return t;
            }
        });

        executor.execute(new CpuMonitor(this));

        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e)->{
            logger.error("Unhandled exception in thread: {}", t.getName(), e);
        });

        environment.events.spawning_complete.add_listener((event)->{
            this.update_state(STATE_RUNNING);
            if (environment.reset_stats) {
                logger.info("Resetting stats");
                this.stats.resetAll();
            }
        });
    }

    public void update_state(State state) {
        this.state = state;
    }

    public void quit() {

    }


    public enum State {
        /**
         *
         */
        STATE_INIT("ready"),
        STATE_SPAWNING("spawning"),
        STATE_RUNNING("running"),
        STATE_CLEANUP("cleanup"),
        STATE_STOPPING("stopping"),
        STATE_STOPPED("stopped"),
        STATE_MISSING("missing");

        public final String CODE;

        private State(String code) {
            this.CODE = code;
        }

        private static Map<String, State> CACHE = new HashMap<>();

        static {
            CACHE.put(STATE_INIT.CODE, STATE_INIT);
            CACHE.put(STATE_SPAWNING.CODE, STATE_SPAWNING);
            CACHE.put(STATE_RUNNING.CODE, STATE_RUNNING);
            CACHE.put(STATE_CLEANUP.CODE, STATE_CLEANUP);
            CACHE.put(STATE_STOPPING.CODE, STATE_STOPPING);
            CACHE.put(STATE_STOPPED.CODE, STATE_STOPPED);
            CACHE.put(STATE_MISSING.CODE, STATE_MISSING);
        }

        public static State parse(String code) {
            return CACHE.get(code);
        }
    }

    private class CpuMonitor implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(CpuMonitor.class);

        private final Runner runner;

        private final OperatingSystemMXBean osBean = getOsBean();

        private CpuMonitor(Runner runner) {
            this.runner = runner;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "cpu-monitor");
            while (true) {
                runner.current_cpu_usage = getCpuUsage();
                if (runner.current_cpu_usage > 90 && !runner.cpu_warning_emitted) {
                    logger.warn(
                            "CPU usage above 90%! This may constrain your throughput and may even give inconsistent response time measurements! See https://docs.locust.io/en/stable/running-locust-distributed.html for how to distribute the load over multiple CPU cores or machines");
                    runner.cpu_warning_emitted = true;
                }

                try {
                    Thread.sleep(LocustConfig.CPU_MONITOR_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private double getCpuUsage() {
            return osBean.getSystemCpuLoad() * 100;
        }

        private OperatingSystemMXBean getOsBean() {
            return (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        }
    }
}
