package com.github.qvp.runners;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.qvp.runners.Runner.State.STATE_INIT;
import static com.github.qvp.runners.Runner.State.STATE_MISSING;
import static com.github.qvp.runners.Runner.State.STATE_RUNNING;
import static com.github.qvp.runners.Runner.State.STATE_SPAWNING;
import static com.github.qvp.runners.Runner.State.STATE_STOPPED;
import static com.github.qvp.runners.Runner.State.STATE_STOPPING;

import com.github.qvp.LocustConfig;
import com.github.qvp.env.Environment;
import com.github.qvp.event.ArgEvent;
import com.github.qvp.event.EnvironmentEvent;
import com.github.qvp.event.MessageEvent;
import com.github.qvp.message.Message;
import com.github.qvp.rpc.Server;
import com.github.qvp.stats.TracebackError;
import com.github.qvp.utils.Utils;
import com.sun.management.OperatingSystemMXBean;

/**
 * A {@link MasterRunner} is a state machine that tells to the master, runs all tasks, collects test results
 * and reports to the master.
 *
 * @author myzhan
 */
public class MasterRunner extends DistributedRunner {

    protected int spawnRate;

    private volatile boolean worker_cpu_warning_emitted;

    private volatile boolean connectionBroken;

    private volatile double currentCpuUsage;
    private volatile boolean cpuWarningEmitted;

    private Map<String, WorkerNode> clients;

    private volatile Server server;

    private Map<String, TracebackError> exceptions = new HashMap<>();


    public MasterRunner(Environment environment) {
        super(environment);
        this.worker_cpu_warning_emitted = false;
        this.clients = new ConcurrentHashMap<>();
    }


    public void init() {
        server = new Server(LocustConfig.masterHost, LocustConfig.masterPort);

        this.executor.execute(new Receiver(this));
        this.executor.execute(new Heartbeat(this));

        this.environment.events.worker_report.add_listener((event) -> {
            String client_id = event.getArg();

            WorkerNode node = clients.get(client_id);
            if (node == null) {
                logger.info("Discarded report from unrecognized worker {}", client_id);
                return;
            }

            Map<String, Object> data = event.getArg();
            node.user_count = ((long) data.get("user_count"));
        });

        this.environment.events.quitting.add_listener((event) -> {
            quit();
        });

        if (LocustConfig.runTime > 0) {
            this.executor.schedule(this::timeLimitStop, LocustConfig.runTime, TimeUnit.SECONDS);
        }
    }

    private void timeLimitStop() {
        logger.info("Time limit reached. Stopping Locust.");
        this.quit();
    }

    public int readyClientCount() {
        return getWorkerCount(STATE_INIT);
    }


    public void startSpawning(int userCount, int spawnRate) {
        this.target_user_count = userCount;

        int numWorkers = getWorkerCount();
        if (numWorkers <= 0) {
            logger.warn(
                    "You are running in distributed mode but have no worker servers connected. Please connect workers prior to swarming.");
            return;
        }

        this.spawnRate = spawnRate;

        int worker_num_users = userCount / numWorkers;
        float worker_spawn_rate = spawnRate / numWorkers;

        int remaining = userCount % numWorkers;

        logger.info(
                "Sending spawn jobs of {} users and {} spawn rate to {} ready clients",
                worker_num_users, worker_spawn_rate, numWorkers);

        if (state != STATE_RUNNING && state != STATE_SPAWNING) {
            this.stats.clearAll();
            this.exceptions = new HashMap<>();
            this.environment.events.test_start.fire(new EnvironmentEvent(this.environment));
        }

        for (WorkerNode client : clients.values()) {
            switch (client.state) {
            case STATE_INIT:
            case STATE_RUNNING:
            case STATE_SPAWNING:
                Map<String, Object> data = new HashMap<>();
                data.put("spawn_rate", worker_spawn_rate);
                data.put("host", environment.host);
                data.put("stop_timeout", environment.stop_timeout);

                int num = worker_num_users;
                if (remaining > 0) {
                    num += 1;
                    remaining -= 1;
                }

                data.put("num_users", num);

                logger.debug("Sending spawn message to client {}", client.id);

                Message m = new Message("spawn", data, client.id);
                try {
                    server.send(m);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                continue;
            default:
            }

        }

        update_state(STATE_SPAWNING);
    }

    public void stop() {
        if (this.state != STATE_INIT
                && this.state != STATE_STOPPED
                && this.state != STATE_STOPPING) {
            logger.info("Stopping...");
            update_state(STATE_STOPPING);

            for (WorkerNode client : clients.values()) {
                try {
                    logger.info("Sending stop message to client {}", client.id);

                    server.send(new Message("stop", null, client.id));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            environment.events.test_stop.fire(new EnvironmentEvent(environment));
        }
    }

    @Override
    public void quit() {
        this.stop();
        logger.info("Quitting...");
        for (WorkerNode client : clients.values()) {
            try {
                logger.info("Sending quit message to client {}", client.id);

                server.send(new Message("quit", null, client.id));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdownNow();
    }

    public void checkStopped() {
        boolean stopped = true;
        if (this.state != STATE_INIT && this.state != STATE_STOPPED) {
            for (WorkerNode client : clients.values()) {
                if (client.state == STATE_RUNNING
                        || client.state == STATE_SPAWNING) {
                    stopped = false;
                }
            }
        }

        if (stopped) {
            update_state(STATE_STOPPED);
        }
    }

    private int getWorkerCount(State type) {
        int num = 0;
        for (WorkerNode client : clients.values()) {
            if (client.state == type) {
                num++;
            }
        }

        return num;
    }

    private int getWorkerCount() {
        int num = 0;
        for (WorkerNode client : clients.values()) {
            switch (client.state) {
            case STATE_INIT:
            case STATE_RUNNING:
            case STATE_SPAWNING:
                num++;
            }
        }

        return num;
    }


    private int getUserCount() {
        int num = 0;
        for (WorkerNode client : clients.values()) {
            num += client.user_count;
        }

        return num;
    }

    private void onMessage(Message message) {
        String type = message.getType();
        String client_id = message.getNodeID();
        try {
            if ("client_ready".equals(type)) {
                WorkerNode client = clients.get(client_id);
                if (client == null) {
                    client = new WorkerNode(client_id, LocustConfig.HEARTBEAT_LIVENESS);
                    clients.put(client_id, client);
                }

                logger.info("Client {} reported as ready. Currently {} clients ready to swarm.",
                        client_id, getWorkerCount());

                if (state == STATE_RUNNING || state == STATE_SPAWNING) {
                    startSpawning(this.target_user_count, this.spawnRate);
                }
            } else if ("client_stopped".equals(type)) {
                clients.remove(client_id);
                logger.info("Removing {} client from running clients", client_id);
            } else if ("heartbeat".equals(type)) {
                WorkerNode client = clients.get(client_id);
                if (client != null) {
                    client.state = State.parse(message.getData().get("state").toString());
                    client.heartbeat.set(LocustConfig.HEARTBEAT_LIVENESS);
                    client.cpu_usage = ((Float) message.getData().get("current_cpu_usage"));

                    if (!client.cpu_warning_emitted) {
                        this.cpuWarningEmitted = true;
                        client.cpu_warning_emitted = true;
                        logger.warn(
                                "Worker {} exceeded cpu threshold (will only log this once per worker)",
                                client.id);
                    }
                }
            } else if ("stats".equals(type)) {
                environment.events.worker_report.fire(new MessageEvent(client_id, message.getData()));
            } else if ("spawning".equals(type)) {
                clients.get(client_id).state = STATE_SPAWNING;
            } else if ("spawning_complete".equals(type)) {
                WorkerNode slaveNode = clients.get(client_id);
                slaveNode.state = STATE_RUNNING;
                slaveNode.user_count = ((long) message.getData().get("count"));
                if (getWorkerCount(STATE_SPAWNING) == 0) {
                    int count = getUserCount();
                    this.environment.events.spawning_complete.fire(ArgEvent.build(count));
                }
            } else if ("quit".equals(type)) {
                WorkerNode node = clients.remove(client_id);
                if (node != null) {
                    logger.info("Client {} quit. Currently {} clients connected.", client_id,
                            getWorkerCount(STATE_INIT));

                    if (getWorkerCount() - getWorkerCount(STATE_MISSING) <= 0) {
                        logger.info("The last worker quit, stopping test.");
                        this.stop();
                    }
                }
            } else if ("exception".equals(type)) {
                String msgStr = message.getData().get("msg").toString();
                String traceback = message.getData().get("traceback").toString();
                logException(client_id, msgStr, traceback);
            }
        } catch (Exception e) {
            logger.error("master runner parse received message error", e);
        }


        checkStopped();
    }

    public void logException(String cid, String msg, String traceback) {
        String key = Utils.md5(traceback);
        TracebackError error = exceptions.get(key);
        if (error == null) {
            error = new TracebackError(msg, traceback);
            exceptions.put(key, error);
        }

        error.addCount();
        error.addWorker(cid);
    }


    public State getState() {
        return this.state;
    }


    private static class Receiver implements Runnable {

        private final MasterRunner runner;

        private Receiver(MasterRunner runner) {
            this.runner = runner;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "receive-from-client");
            while (true) {
                try {
                    Message message = runner.server.recv();
                    if (message == null) {
                        continue;
                    }
                    runner.connectionBroken = false;
                    runner.onMessage(message);
                } catch (Exception ex) {
                    logger.error("RPCError found when receiving from client", ex);
                    runner.connectionBroken = true;
                    try {
                        Thread.sleep(LocustConfig.FALLBACK_INTERVAL);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
    }

    private class Heartbeat implements Runnable {

        private final MasterRunner runner;

        private final OperatingSystemMXBean osBean = getOsBean();

        private Heartbeat(MasterRunner runner) {
            this.runner = runner;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + "heartbeat");
            while (true) {
                try {
                    Thread.sleep(LocustConfig.HEARTBEAT_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }

                if (runner.connectionBroken) {
                    runner.resetConnection();
                    continue;
                }

                for (WorkerNode node : clients.values()) {
                    if (node.heartbeat.get() < 0 && node.state != STATE_MISSING) {
                        logger.info(
                                "Worker {} failed to send heartbeat, setting state to missing.",
                                node.id);
                        node.state = STATE_MISSING;
                        node.user_count = 0;

                        if (getWorkerCount() - getWorkerCount(STATE_MISSING) <= 0) {
                            logger.info("The last worker went missing, stopping test.");
                            runner.stop();
                            runner.checkStopped();
                        }

                    } else {
                        node.heartbeat.decrementAndGet();
                    }
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


    private void resetConnection() {
        logger.info("Reset connection to worker");

        try {
            this.server.close();
            this.server = new Server(LocustConfig.masterHost, LocustConfig.masterPort);
        } catch (Exception e) {
            logger.error("Temporary failure when resetting connection, will retry later.", e);
        }
    }

    private static class WorkerNode {

        public WorkerNode(String id, int heartbeat_liveness) {
            this.id = id;
            this.state = STATE_INIT;
            this.heartbeat.set(heartbeat_liveness);
        }

        public String id;

        public State state;
        public AtomicLong heartbeat = new AtomicLong();
        public long user_count;

        public double cpu_usage;

        public boolean cpu_warning_emitted;
    }

}
