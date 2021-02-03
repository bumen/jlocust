package com.github.qvp.event;

/**
 * @date 2021-02-01
 * @author zhangyuqiang02@playcrab.com
 */
public class ArgEvent implements Event{

    protected final Object[] args;

    private int idx;

    public ArgEvent(Object[] args) {
        this.args = args;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T getArg() {
        return (T) args[idx++];
    }

    @Override
    public void reset() {
        idx = 0;
    }

    public static ArgEvent build(Object... args) {
        return new ArgEvent(args);
    }
}
