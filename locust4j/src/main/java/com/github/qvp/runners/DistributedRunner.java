package com.github.qvp.runners;

import com.github.qvp.env.Environment;
import com.github.qvp.stats.StatsService;

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
