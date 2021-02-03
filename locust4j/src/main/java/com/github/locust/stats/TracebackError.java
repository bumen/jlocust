package com.github.locust.stats;

import java.util.HashSet;
import java.util.Set;

/**
 * @date 2021-01-30
 * @author zhangyuqiang02@playcrab.com
 */
public class TracebackError {

    private int count;

    private String msg;

    private String traceback;

    private Set<String> workers = new HashSet<>();

    public TracebackError(String msg, String traceback) {
        this.msg = msg;
        this.traceback = traceback;
    }

    public void addWorker(String cid) {
        this.workers.add(cid);
    }

    public void addCount() {
        this.count++;
    }
}
