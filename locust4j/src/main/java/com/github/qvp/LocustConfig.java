package com.github.qvp;

import java.io.IOException;

import com.github.qvp.utils.PropertyUtils;
import com.github.qvp.utils.PropertyUtils.PropertiesHelper;

/**
 * @date 2021-01-29
 * @author zhangyuqiang02@playcrab.com
 */
public class LocustConfig {

    public static String SYSTEM_CONFIG_PROPERTIES_FILE = "locust.configurationFile";

    /**
     * 压测数量
     */
    public static int UserCount;


    /**
     * 压测增长率
     */
    public static int SpawnRate;

    /**
     * 几个子节点
     */
    public static int WorkerCount;

    /**
     * 运行时间，只能master和single使用
     */
    public static int RunTime;

    public static int HEARTBEAT_LIVENESS = 3;

    public static int HEARTBEAT_INTERVAL = 1000;

    public static int CPU_MONITOR_INTERVAL = 5000;

    public static int FALLBACK_INTERVAL = 5000;

    public static int CONSOLE_STATS_INTERVAL_SEC = 2000;

    public static int CURRENT_RESPONSE_TIME_PERCENTILE_WINDOW = 10;

    public static String MasterHost = "127.0.0.1";

    public static int MasterPort = 5557;

    public static boolean ResetState = true;
    public static boolean use_response_times_cache;


    public static void load() {
        String path = System.getProperty(SYSTEM_CONFIG_PROPERTIES_FILE);
        if (path == null || path.equals("")) {
            path = "config/config.properties";
        }

        try {
            PropertiesHelper properties = PropertyUtils.readPropertiesFile(path);
            UserCount = properties.getIntValue("boomer.user.count", 1);
            SpawnRate = properties.getIntValue("boomer.spawn.rate", 1);
            RunTime = properties.getIntValue("server.runtime");
            WorkerCount = properties.getIntValue("server.worker.count", 1);
            MasterHost = properties.getString("server.host", "127.0.0.1");
            MasterPort = properties.getIntValue("server.port", 5557);
        } catch (IOException e) {
            throw new IllegalStateException("load locust config error", e);
        }

    }
}
