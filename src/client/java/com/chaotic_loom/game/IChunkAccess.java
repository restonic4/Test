package com.chaotic_loom.game;

import org.joml.Vector3ic;

/**
 * Interface provided to Chunks allowing them to query block data
 * potentially outside their own boundaries (neighbor lookups).
 */
public interface IChunkAccess {
    /** Gets the BlockType at world block coordinates relative to the entity origin. */
    BlockType getBlockType(int worldX, int worldY, int worldZ);

    /** Gets the BlockType at world block coordinates relative to the entity origin. */
    default BlockType getBlockType(Vector3ic worldBlockPos) {
        return getBlockType(worldBlockPos.x(), worldBlockPos.y(), worldBlockPos.z());
    }

    /** Marks a neighboring chunk as potentially needing a mesh update (Client only). */
    void markNeighborDirty(Vector3ic originChunkPos, int dx, int dy, int dz);

    // Environment check might be needed if meshing logic lives in common chunk
    // Environment getEnvironment();
}