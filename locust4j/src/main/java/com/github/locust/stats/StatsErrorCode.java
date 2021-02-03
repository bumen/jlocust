package com.github.locust.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * @date 2020-07-17
 * @author zhangyuqiang02@playcrab.com
 */
public enum StatsErrorCode {
    /**
     * 成功
     */
    SUCCESS(0, "success"),
    /**
     * 请求超时
     */
    REQ_TIMEOUT(1, "req_timeout"),
    /**
     * 请求失败
     */
    REQ_ERROR(2, "req_error"),
    /**
     * 连接超时
     */
    CONNECT_TIMEOUT(3, "con_timeout"),
    /**
     * 连接失败
     */
    CONNECT_ERROR(4, "con_error"),
    /**
     * http 登录请求错误
     */
    LOGIN_ERROR(5, "login_error"),
    /**
     * http 路由请求错误
     */
    ROUTE_ERROR(6, "route_error"),
    /**
     * 服务器业务处理异常
     */
    BZ_ERROR(7, "bz_error"),

    ERROR(500, "error");


    public final int CODE;

    public final String STR;

    private static Map<Integer,StatsErrorCode> codeLookup = new HashMap<>();

    static {
        for (StatsErrorCode type : StatsErrorCode.values()) {
            codeLookup.put(type.CODE, type);
        }
    }


    StatsErrorCode(int code, String str) {
        this.CODE = code;
        this.STR = str;
    }

    public static StatsErrorCode forCode(int code)  {
        return codeLookup.get(code);
    }
}
