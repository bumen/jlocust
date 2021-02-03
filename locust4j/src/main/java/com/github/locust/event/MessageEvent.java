package com.github.locust.event;

import java.util.Map;

/**
 * @date 2021-02-01
 * @author zhangyuqiang02@playcrab.com
 */
public class MessageEvent extends ArgEvent {

    public MessageEvent(String clientId, Map<String, Object> data) {
        super(new Object[]{clientId, data});
    }
}
