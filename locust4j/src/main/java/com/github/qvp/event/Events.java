package com.github.qvp.event;

import java.util.ArrayList;
import java.util.List;

/**
 * @date 2021-02-01
 * @author zhangyuqiang02@playcrab.com
 */
public class Events {

    public final EventHook request_success = new EventHook();
    public final EventHook request_failure = new EventHook();
    public final EventHook user_error = new EventHook();
    public final EventHook report_to_master = new EventHook();
    public final EventHook worker_report = new EventHook();
    public final EventHook spawning_complete = new EventHook();
    public final EventHook quitting = new EventHook();
    public final EventHook init = new EventHook();
    public final EventHook init_command_line_parser = new EventHook();
    public final EventHook test_start = new EventHook();
    public final EventHook test_stop = new EventHook();
    public final EventHook reset_stats = new EventHook();


    public static class EventHook {
        private List<EventHandler> handlers;

        private EventHook() {
            this.handlers = new ArrayList<>();
        }

        public EventHandler add_listener(EventHandler handler) {
            this.handlers.add(handler);
            return handler;
        }

        public void remove_listener(EventHandler handler) {
            this.handlers.remove(handler);
        }

        public void fire(Event event) {
            fire(event, false);
        }

        public void fire(Event event, boolean reverse) {
            try {
                if (reverse) {
                    for (int i = handlers.size() - 1; i>= 0; i--) {
                        EventHandler handler = handlers.get(i);
                        handler.handle(event);
                    }
                } else {
                    for (EventHandler handler : handlers) {
                        handler.handle(event);
                        event.reset();
                    }
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
