package com.github.qvp.event;

import com.github.qvp.env.Environment;

/**
 * @date 2021-02-01
 * @author zhangyuqiang02@playcrab.com
 */
public class EnvironmentEvent extends ArgEvent {


    public EnvironmentEvent(Environment environment) {
        super(new Object[]{environment});
    }
}
