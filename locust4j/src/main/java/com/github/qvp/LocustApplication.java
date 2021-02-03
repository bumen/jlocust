package com.github.qvp;

/**
 * @date 2021-01-30
 * @author zhangyuqiang02@playcrab.com
 */
public class LocustApplication {

    public static void main(String[] args) {
        LocustConfig.load();

        LocustMaster.getInstance().run();
    }
}
