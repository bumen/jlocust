package com.github.qvp.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @date 2020-07-16
 * @author zhangyuqiang02@playcrab.com
 */
public class RequestStats {

    private TreeMap<String, StatsEntry> entries;
    private Map<String, StatsError> errors;

    private StatsEntry total;

    private boolean use_response_times_cache = true;

    private List<Object> history;

    public RequestStats() {
        this(true);
    }

    /**
     * worker node cache false
     * @param use_response_times_cache c
     */
    public RequestStats(boolean use_response_times_cache) {
        this.use_response_times_cache = use_response_times_cache;

        entries = new TreeMap<>();
        errors = new HashMap<>();
        total = new StatsEntry(this, "Aggregated", "",use_response_times_cache);
        this.history = new ArrayList<>();
    }

    public long lastRequestTimestamp() {
        return this.total.getLastRequestTimestamp();
    }

    public long getStartTime() {
        return this.total.getStartTime();
    }

    public void resetAll() {
        this.total.reset();
        this.errors = new HashMap<>();
        for (Map.Entry<String, StatsEntry> entrys : entries.entrySet()) {
            entrys.getValue().reset();
        }

        this.history = new ArrayList<>();
    }

    public void clearAll() {
        this.total = new StatsEntry(this, "Aggregated", "", this.use_response_times_cache);
        this.entries = new TreeMap<>();
        this.errors = new HashMap<>();
        this.history = new ArrayList<>();
    }

    public TreeMap<String, StatsEntry> getEntries() {
        return entries;
    }

    public Map<String, StatsError> getErrors() {
        return errors;
    }

    public StatsEntry getTotal() {
        return total;
    }


}
