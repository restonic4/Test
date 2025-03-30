package com.chaotic_loom.game.core;

public interface Timer {
    /** Initialize the timer. */
    void init();

    /**
     * Returns the elapsed time (delta time) since the last call, and performs internal updates.
     * Essential for both client (rendering, variable updates) and server (accumulator).
     * @return elapsed time in seconds.
     */
    float getElapsedTime();
}
