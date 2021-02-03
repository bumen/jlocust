package com.github.locust.runners;

/**
 * State of runner
 *
 * @author myzhan
 */
public enum RunnerState {
    /**
     * Runner is ready to receive message from master.
     */
    Ready,

    Spawning,

    Running,

    Cleanup,

    Stopping,

    Stopped,

    Missing
}
