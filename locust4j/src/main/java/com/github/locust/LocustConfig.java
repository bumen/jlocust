package com.github.locust;

/**
 * @date 2021-01-29
 * @author zhangyuqiang02@playcrab.com
 */
public class LocustConfig {

    /**
     * 压测数量
     */
    public static int UserCount;

    /**
     * 压测增长率
     */
    public static int spawnRate;

    /**
     * 几个子节点
     */
    public static int workerCount;

    /**
     * 运行时间，只能master和single使用
     */
    public static int runTime;

    public static int HEARTBEAT_LIVENESS = 3;

    public static int HEARTBEAT_INTERVAL = 1000;

    public static int CPU_MONITOR_INTERVAL = 5000;

    public static int FALLBACK_INTERVAL = 5000;

    public static int CONSOLE_STATS_INTERVAL_SEC = 2000;

    public static int CURRENT_RESPONSE_TIME_PERCENTILE_WINDOW = 10;

    public static String masterHost = "127.0.0.1";

    public static int masterPort = 5557;

    public static boolean ResetState = true;
    public static boolean use_response_times_cache;
}
