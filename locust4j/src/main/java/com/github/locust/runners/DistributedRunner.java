package com.github.locust.runners;

import com.github.locust.env.Environment;
import com.github.locust.stats.StatsService;

/**
 * @date 2021-02-01
 * @author zhangyuqiang02@playcrab.com
 */
public abstract class DistributedRunner extends Runner {

    public DistributedRunner(Environment environment) {
        super(environment);

        StatsService.INSTANCE
                .setup_distributed_stats_event_listeners(environment.events, environment.stats);
    }
}
