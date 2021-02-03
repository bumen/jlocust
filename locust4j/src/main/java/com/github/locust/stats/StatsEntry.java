package com.github.locust.stats;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.github.locust.LocustConfig;

/**
 * @date 2020-07-16
 * @author zhangyuqiang02@playcrab.com
 */
public class StatsEntry {

    private String name;

    private String method;

    private long numRequests;

    private long num_none_requests;

    private long numFailures;

    private long totalResponseTime;

    private long minResponseTime;

    private long maxResponseTime;

    private HashMap<Long, Long> numReqsPerSec;

    private HashMap<Long, Long> numFailPerSec;

    private TreeMap<Long, Long> responseTimes;

    private boolean useResponseTimesCache;

    private TreeMap<Long, CachedResponseTimes> responseTimesCache;

    private long totalContentLength;

    private long startTime;

    private long lastRequestTimestamp;

    private RequestStats stats;

    public StatsEntry(RequestStats stats, String name, String method,
            boolean useResponseTimesCache) {
        this.stats = stats;
        this.name = name;
        this.method = method;
        this.useResponseTimesCache = useResponseTimesCache;
        this.reset();
    }

    public void reset() {
        this.startTime = Instant.now().toEpochMilli();
        this.numRequests = 0;
        this.numFailures = 0;
        this.totalResponseTime = 0;
        this.responseTimes = new TreeMap<>();
        this.minResponseTime = 0;
        this.maxResponseTime = 0;
        this.lastRequestTimestamp = 0;
        this.numReqsPerSec = new HashMap<>();
        this.numFailPerSec = new HashMap<>();
        this.totalContentLength = 0;

        if (this.useResponseTimesCache) {
            this.responseTimesCache = new TreeMap<>();
            cacheResponseTimes((int) (startTime));
        }
    }

    public double failRatio() {
        try {
            return numFailures / (numRequests * 1D);
        } catch (Exception e) {
            if (numFailures > 0) {
                return 1d;
            }

            return 0d;
        }
    }

    public double avgResponseTime() {
        if (numRequests == 0) {
            return 0D;
        }

        try {
            return totalResponseTime / (numRequests - num_none_requests);
        } catch (Exception e) {
            return 0D;
        }
    }

    public long medianResponseTime() {
        if (responseTimes == null || responseTimes.isEmpty()) {
            return 0;
        }

        long median = median_from_dict(numRequests - num_none_requests, responseTimes);
        if (median > maxResponseTime) {
            median = maxResponseTime;
        } else if (median < minResponseTime) {
            median = minResponseTime;
        }

        return median;
    }

    private static long median_from_dict(long total, Map<Long, Long> count) {
        long pos = (total - 1L) / 2L;
        for (Entry<Long, Long> entry : count.entrySet()) {
            if (pos < entry.getValue()) {
                return entry.getKey();
            }

            pos -= entry.getValue();
        }

        return 0;
    }

    public double currentRps() {
        long last = stats.lastRequestTimestamp();
        if (last == 0L) {
            return 0;
        }

        long sliceStartTime = Math.max(last - 12, stats.getStartTime());

        double total = 0, c = 0;
        for (long i = sliceStartTime, l = last - 2; i <= l; i++) {
            Long v = numReqsPerSec.get(i);
            if (v == null) {
                v = 0L;
            }

            total += v;
            c++;
        }

        if (total == 0) {
            return 0;
        }

        return total / c;
    }

    public double currentFailPerSec() {
        long last = stats.lastRequestTimestamp();
        if (last == 0L) {
            return 0;
        }

        long sliceStartTime = Math.max(last - 12, stats.getStartTime());

        double total = 0, c = 0;
        for (long i = sliceStartTime, l = last - 2; i <= l; i++) {
            Long v = numFailPerSec.get(i);
            if (v == null) {
                v = 0L;
            }

            total += v;
            c++;
        }

        if (total == 0) {
            return 0;
        }

        return total / c;
    }

    public double totalRps() {
        if (stats.lastRequestTimestamp() == 0L || stats.getStartTime() == 0L) {
            return 0d;
        }

        return numRequests / Math
                .max(stats.lastRequestTimestamp() - stats.getStartTime(), 1);
    }

    public double totalFailPerSec() {
        if (stats.lastRequestTimestamp() == 0L || stats.getStartTime() == 0L) {
            return 0d;
        }
        return numFailures / Math
                .max(stats.lastRequestTimestamp() - stats.getStartTime(), 1);
    }

    public long avgContentLength() {
        if (this.numRequests == 0L) {
            return 0;
        }

        return this.totalContentLength / this.numRequests;
    }

    public void extend(StatsEntry other) {
        long oldLastRequestTimestamp = this.lastRequestTimestamp;

        this.lastRequestTimestamp = Math.max(other.lastRequestTimestamp, lastRequestTimestamp);
        if (this.startTime == 0L) {
            this.startTime = other.startTime;
        }
        this.startTime = Math.min(other.startTime, startTime);

        this.numRequests = other.numRequests + numRequests;
        this.num_none_requests = this.num_none_requests + other.num_none_requests;
        this.numFailures = other.numFailures + numFailures;
        this.totalResponseTime = other.totalResponseTime + totalResponseTime;
        this.maxResponseTime = Math.max(other.maxResponseTime, maxResponseTime);
        if (this.minResponseTime == 0) {
            this.minResponseTime = other.minResponseTime;
        }
        this.minResponseTime = Math.min(minResponseTime, other.minResponseTime);
        this.totalContentLength = other.totalContentLength + totalContentLength;

        Map<Long, Long> responseTimes = other.responseTimes;
        for (Map.Entry<Long, Long> entry : responseTimes.entrySet()) {
            Long v = this.responseTimes.get(entry.getKey());
            if (v == null) {
                v = 0L;
            }

            this.responseTimes.put(entry.getKey(), v + entry.getValue());
        }

        Map<Long, Long> secReqs = other.numReqsPerSec;
        for (Map.Entry<Long, Long> entry : secReqs.entrySet()) {
            Long v = this.numReqsPerSec.get(entry.getKey());
            if (v == null) {
                v = 0L;
            }

            this.numReqsPerSec.put(entry.getKey(), v + entry.getValue());
        }

        Map<Long, Long> secFails = other.numFailPerSec;
        for (Map.Entry<Long, Long> entry : secFails.entrySet()) {
            Long v = this.numFailPerSec.get(entry.getKey());
            if (v == null) {
                v = 0L;
            }

            this.numFailPerSec.put(entry.getKey(), v + entry.getValue());
        }

        if (useResponseTimesCache) {
            long last_time = this.lastRequestTimestamp;
            if (this.lastRequestTimestamp > oldLastRequestTimestamp) {
                this.cacheResponseTimes(last_time);
            }
        }

    }


    public static StatsEntry unserialize(Map<String, Object> data) {
        String name = data.get("name").toString();
        String method = data.get("method").toString();
        StatsEntry entry = new StatsEntry(null, name, method, false);

        entry.lastRequestTimestamp = (long) data.get("last_request_timestamp");
        entry.startTime = (long) data.get("start_time");
        entry.numRequests = ((long) data.get("num_requests"));
        entry.num_none_requests = ((long) data.get("num_none_requests"));
        entry.numFailures = ((long) data.get("num_failures"));
        entry.totalResponseTime = ((long) data.get("total_response_time"));
        entry.maxResponseTime = ((long) data.get("max_response_time"));
        entry.minResponseTime = ((long) data.get("min_response_time"));
        entry.totalContentLength = ((long) data.get("total_content_length"));
        Map<Long, Long> responseTimes = (Map<Long, Long>) data.get("response_times");
        entry.responseTimes = new TreeMap<>(responseTimes);
        HashMap<Long, Long> tmp = (HashMap<Long, Long>) data.get("num_reqs_per_sec");
        entry.numReqsPerSec = new HashMap<>(tmp);
        tmp = (HashMap<Long, Long>) data.get("num_fail_per_sec");
        entry.numFailPerSec = new HashMap<>(tmp);
        return entry;
    }


    public String to_string(boolean current) {
        double rps, fail_per_sec;
        if (current) {
            rps = this.currentRps();
            fail_per_sec = this.currentFailPerSec();
        } else {
            rps = this.totalRps();
            fail_per_sec = this.totalFailPerSec();
        }

        String fmt = "|%14s|%11d|%17s|%10.1f|%10d|%10d|%13d|%12.1f|%12.1f|";

        String fail = failPercent();

        return String
                .format(fmt, method + name, numRequests, fail, avgResponseTime(), minResponseTime,
                        maxResponseTime, medianResponseTime(), rps, fail_per_sec);
    }


    public String failPercent() {
        String fail = "0(0.00%)";
        if (numRequests == 0) {
            return fail;
        }

        try {

            double failPercent = failRatio() * 100D;

            fail = String.format("%d(%.2f%%)", numFailures, failPercent);
        } catch (Exception e) {
            //
        }

        return fail;
    }

    public long get_response_time_percentile(double percent) {
        return calculate_response_time_percentile(responseTimes, numRequests, percent);
    }

    private static long calculate_response_time_percentile(TreeMap<Long, Long> responseTimes,
            long numRequests, double percent) {

        int num_of_request = (int) (numRequests * percent);

        int processed_count = 0;

        for (Entry<Long, Long> entry : responseTimes.descendingMap().entrySet()) {
            processed_count += entry.getValue();

            if (numRequests - processed_count <= num_of_request) {
                return entry.getKey();
            }
        }

        return 0;
    }

    public String percentile() {
        String fmt = "|%14s|%11d|%10d|%10d|%10d|%10d|%10d|%10d|%10d|%10d|%11d|";

        long five = get_response_time_percentile(0.5);
        long six = get_response_time_percentile(0.66);
        long seven = get_response_time_percentile(0.75);
        long eight = get_response_time_percentile(0.80);
        long nine = get_response_time_percentile(0.90);
        long ninetyFive = get_response_time_percentile(0.95);
        long ninetyEight = get_response_time_percentile(0.98);
        long ninetyNine = get_response_time_percentile(0.99);
        long all = get_response_time_percentile(1.00);
        return String
                .format(fmt, method + name, numRequests, five, six, seven, eight, nine, ninetyFive,
                        ninetyEight, ninetyNine, all);
    }


    public void cacheResponseTimes(long time) {
        this.responseTimesCache
                .put(time, new CachedResponseTimes(this.responseTimes, this.numRequests));

        int cacheSize = LocustConfig.CURRENT_RESPONSE_TIME_PERCENTILE_WINDOW + 10;

        if (responseTimesCache.size() > cacheSize) {
            Long last = responseTimesCache.lastKey();
            responseTimesCache.remove(last);
        }

    }

    private static class CachedResponseTimes {

        public final TreeMap<Long, Long> responseTimes;
        public final long numRequests;

        public CachedResponseTimes(TreeMap<Long, Long> responseTimes, long numRequests) {
            this.responseTimes = new TreeMap<>(responseTimes);
            this.numRequests = numRequests;
        }
    }

    public long getNumRequests() {
        return numRequests;
    }

    public long getNumFailures() {
        return numFailures;
    }


    public Map<Long, Long> getResponseTimes() {
        return responseTimes;
    }


    public long getLastRequestTimestamp() {
        return lastRequestTimestamp;
    }

    public String getName() {
        return name;
    }

    public String getMethod() {
        return method;
    }

    public long getStartTime() {
        return startTime;
    }
}
