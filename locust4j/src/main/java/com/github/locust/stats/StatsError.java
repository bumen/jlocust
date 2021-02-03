package com.github.locust.stats;

import java.util.Map;

/**
 * @date 2020-07-16
 * @author zhangyuqiang02@playcrab.com
 */
public class StatsError {

    private String method;

    private String name;

    private String error;

    private long occurrences;

    public StatsError(String method, String name, String error, long occurrences) {
        this.method = method;
        this.name = name;
        this.error = error;
        this.occurrences = occurrences;
    }


    public static String createKey(String method, String name, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(".").append(name).append(".").append(error);
        return sb.toString();
    }

    public void occurred() {
        this.occurrences += 1;
    }

    public long getOccurrences() {
        return this.occurrences;
    }

    public String toName() {
        return method + "." + name + "." + error;
    }

    public void to_dict() {
        //TODO
    }

    public static StatsError from_dict(Map<String, Object> data) {
        return new StatsError(data.get("method").toString(), data.get("name").toString(),
                data.get("error").toString(), (long) data.get("occurrences"));
    }

    public String getName() {
        return name;
    }

    public void extend(long occurrences) {
        this.occurrences += occurrences;
    }
}
