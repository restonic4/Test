package com.chaotic_loom.game.core;

import com.chaotic_loom.game.networking.NettyServerHelper;
import com.chaotic_loom.game.networking.packets.LoginPacket;
import com.chaotic_loom.game.registries.built_in.Packets;
import io.netty.channel.Channel;

public class ServerEngine extends AbstractEngine {
    private final ServerTimer timer;

    public ServerEngine() {
        super(Environment.SERVER);

        this.timer = new ServerTimer();
    }

    @Override
    protected void init() {
        timer.init();

        Channel serverChannel = NettyServerHelper.init();
        getNetworkingManager().setChannel(serverChannel);

        getLogger().info("Server Engine Initialized.");
    }

    @Override
    protected void gameLoop() {
        getLogger().info("Starting server game loop...");

        final float interval = 1f / ServerConstants.TARGET_UPS; // Fixed time step interval

        while (true) { // TODO: Implement proper shutdown condition
            // Get time delta - needed to advance accumulator
            // Also processes UPS counter updates internally now
            timer.getElapsedTime(); // From ServerTimer

            // Process network messages asynchronously or polled here
            // networkManager.processIncomingMessages();

            // Fixed timestep update loop for server logic
            // Use the getAccumulatedTime/subtractAccumulatedTime specific to ServerTimer
            while (timer.getAccumulatedTime() >= interval) {
                // Update server-specific game state using fixed interval
                updateServer(interval);

                timer.logicUpdated(); // Increment UPS counter
                // Subtract time *after* update
                timer.subtractAccumulatedTime(interval);
            }

            // Sleep/Yield logic to avoid 100% CPU usage
            // Calculate how long until the next *potential* tick start time
            // This is tricky because updates might take variable time.
            // A simpler approach is to just sleep for a small fixed duration if no updates ran.
            // Or calculate sleep based on when the *next* tick should ideally start.

            double nextTickStartTime = calculateNextTickStartTime(interval);
            double timeToWait = nextTickStartTime - timer.getTime(); // Using ServerTimer's getTime()

            if (timeToWait > 0) {
                try {
                    // Sleep for slightly less to account for scheduling delays
                    long sleepMillis = Math.max(1, (long)((timeToWait - 0.001) * 1000));
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            } else if (timeToWait < -interval * 5) {
                // Lag detection: If we're severely behind, log it and maybe reset timer?
                getLogger().warn("Server can't keep up! Running behind schedule.");
                // Consider resetting accumulated time or just letting it catch up.
            } else {
                // If we are slightly behind or right on time, yield briefly
                Thread.yield();
            }
        }
    }

    // Helper to estimate when the next tick *should* start
    private double lastTickTime = 0; // Track when the last tick *processing* finished or started
    private double calculateNextTickStartTime(float interval) {
        if (lastTickTime == 0) lastTickTime = timer.getTime(); // Initialize on first call
        // Ideal next start time based on fixed interval
        // This assumes the loop starts roughly aligned with ticks initially
        // A more robust method might track total ticks elapsed.
        // Simple version: assume next tick should start 'interval' after the last one ideally started.
        // This needs refinement based on exact loop needs.
        // Let's just return a time slightly in the future to ensure sleep.
        return timer.getTime() + (interval / 4.0); // Simple yield/sleep strategy control
    }


    // Server-specific update logic
    private void updateServer(float interval) {
        // Update world state based on player inputs, AI, physics simulation
        // worldManager.update(interval);
        // Process player actions received over network

        // System.out.println("Server Tick Update: " + interval);

        // Broadcast world state updates to clients (might be done less frequently)
        // networkManager.broadcastGameState(worldManager.getCurrentState());
    }

    @Override
    protected void cleanup() {
        getLogger().info("Cleaning up server engine...");
        // networkManager.shutdown();
        // worldManager.saveState();
        getLogger().info("Server Engine Cleaned Up.");
    }

    @Override
    protected Timer getTimer() {
        return this.timer;
    }
}