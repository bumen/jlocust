package com.github.qvp.event;

/**
 * @date 2021-02-01
 * @author zhangyuqiang02@playcrab.com
 */
public interface Event {

    <T> T getArg();

    void reset();
}
