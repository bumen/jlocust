package com.github.qvp.env;

import com.github.qvp.event.Events;
import com.github.qvp.runners.MasterRunner;
import com.github.qvp.runners.Runner;
import com.github.qvp.shape.LoadTestShape;
import com.github.qvp.stats.RequestStats;
import com.github.qvp.user.User;

/**
 * @date 2021-02-01
 * @author zhangyuqiang02@playcrab.com
 */
public class Environment {

    public Events events;

    public Class<User> user_classes;

    public Class<LoadTestShape> shape_class;

    public String tags;

    public String exclude_tags;

    public RequestStats stats;

    public Runner runner;

    // todo
    public Object web_ui;

    public String host;

    public boolean reset_stats;

    public int stop_timeout;

    public boolean catch_exceptions;


    public int process_exit_code;


    public Environment() {
        this(new Events(), null, null, null, null, null, "", false, 0, false);
    }


    public Environment(Events events, Class<User> user_classes,
            Class<LoadTestShape> shape_class, String tags, String exclude_tags,
            Object web_ui, String host, boolean reset_stats,
            int stop_timeout, boolean catch_exceptions) {
        this.events = events;
        this.user_classes = user_classes;
        this.shape_class = shape_class;
        this.tags = tags;
        this.exclude_tags = exclude_tags;
        this.stats = new RequestStats();
        this.web_ui = web_ui;
        this.host = host;
        this.reset_stats = reset_stats;
        this.stop_timeout = stop_timeout;
        this.catch_exceptions = catch_exceptions;
    }


    public MasterRunner create_master_runner(String master_bind_host, int master_bind_port) {
        if (this.runner != null) {
            throw new IllegalStateException("Environment.runner already exists");
        }

        MasterRunner runner = new MasterRunner(this);
        runner.init();

        this.runner = runner;
        return runner;
    }
}
