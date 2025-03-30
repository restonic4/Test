package com.chaotic_loom.game.core;

public class ServerTimer implements Timer {
    private double lastLoopTime;
    private double accumulatedTime;
    private int upsCount;
    private double lastUpsTime;

    @Override
    public void init() {
        lastLoopTime = getTime();
        lastUpsTime = getTime();
        accumulatedTime = 0;
        upsCount = 0;
    }

    public double getTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    @Override
    public float getElapsedTime() {
        double currentTime = getTime();
        float elapsedTime = (float) (currentTime - lastLoopTime);
        lastLoopTime = currentTime;

        // Server accumulates time for its fixed update loop
        accumulatedTime += elapsedTime;

        // Update UPS counter display each second (called by logicUpdated)
        updateUPSCounter();

        return elapsedTime;
    }

    // Called externally by ServerEngine after a logic update
    public void logicUpdated() {
        upsCount++;
    }

    private void updateUPSCounter() {
        // Check only when logic is updated
        if (getTime() - lastUpsTime >= 1.0) {
            upsCount = 0;
            lastUpsTime += 1.0;
        }
    }

    public double getAccumulatedTime() {
        return accumulatedTime;
    }

    public void subtractAccumulatedTime(double interval) {
        accumulatedTime = Math.max(0, accumulatedTime - interval);
    }

    public int getUpsCount() {
        return upsCount;
    }
}