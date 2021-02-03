package com.github.locust;

/**
 * @date 2021-01-30
 * @author zhangyuqiang02@playcrab.com
 */
public class LocustApplication {

    public static void main(String[] args) {
        LocustConfig.UserCount = 10;
        LocustConfig.workerCount = 1;
        LocustConfig.spawnRate = 1;

        LocustMaster.getInstance().run();
    }
}
