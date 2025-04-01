package com.chaotic_loom.game.rendering;

public class RenderStats {
    // Frame Stats (Reset each frame)
    private long frameCount = 0; // Total frames rendered since reset/start
    private int drawCallsThisFrame = 0;
    private int atlasBindsThisFrame = 0;
    private int meshBindsThisFrame = 0; // VAO binds
    private int instancesDrawnThisFrame = 0;
    private int batchesProcessedThisFrame = 0; // Number of entries in the outer batch map

    // --- Methods to be called by Renderer ---

    /** Resets the per-frame counters. Call at the beginning of each frame's render prep. */
    public void resetFrame() {
        frameCount++; // Increment total frame count
        drawCallsThisFrame = 0;
        atlasBindsThisFrame = 0;
        meshBindsThisFrame = 0;
        instancesDrawnThisFrame = 0;
        batchesProcessedThisFrame = 0;
    }

    public void recordAtlasBind() {
        atlasBindsThisFrame++;
    }

    public void recordMeshBind() {
        meshBindsThisFrame++;
    }

    public void recordDrawCall() {
        drawCallsThisFrame++;
    }

    public void recordInstancesRendered(int instanceCount) {
        instancesDrawnThisFrame += instanceCount;
    }

    public void recordBatchProcessed() {
        batchesProcessedThisFrame++;
    }

    public void recordBatchProcessed(int amount) {
        batchesProcessedThisFrame += amount;
    }

    // --- Getters for Displaying Stats ---

    public long getTotalFrames() { return frameCount; }
    public int getDrawCallsThisFrame() { return drawCallsThisFrame; }
    public int getAtlasBindsThisFrame() { return atlasBindsThisFrame; }
    public int getMeshBindsThisFrame() { return meshBindsThisFrame; }
    public int getInstancesDrawnThisFrame() { return instancesDrawnThisFrame; }
    public int getBatchesProcessedThisFrame() { return batchesProcessedThisFrame; }

    /** Generates a formatted string summary of the last frame's stats. */
    public String getSummary() {
        return String.format("Frame[%d]: DrawCalls=%d, Instances=%d, AtlasBinds=%d, MeshBinds=%d, Batches=%d",
                frameCount,
                drawCallsThisFrame,
                instancesDrawnThisFrame,
                atlasBindsThisFrame,
                meshBindsThisFrame,
                batchesProcessedThisFrame);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
