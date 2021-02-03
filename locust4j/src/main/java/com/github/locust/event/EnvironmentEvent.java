package com.github.locust.event;

import com.github.locust.env.Environment;

/**
 * @date 2021-02-01
 * @author zhangyuqiang02@playcrab.com
 */
public class EnvironmentEvent extends ArgEvent {


    public EnvironmentEvent(Environment environment) {
        super(new Object[]{environment});
    }
}
