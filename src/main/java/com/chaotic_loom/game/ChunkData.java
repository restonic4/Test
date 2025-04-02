package com.chaotic_loom.game;

import static com.chaotic_loom.game.BlockTypes.*;

public class ChunkData {

    // Core data: 3D array of block type IDs.
    private final byte[][][] blocks;

    // Optional: Store chunk's position in the world grid
    private final int chunkX, chunkY, chunkZ;

    /**
     * Creates a new ChunkData object, initializing the block array.
     * @param chunkX World grid X coordinate of the chunk.
     * @param chunkY World grid Y coordinate of the chunk.
     * @param chunkZ World grid Z coordinate of the chunk.
     */
    public ChunkData(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        // Initialize with air by default
        this.blocks = new byte[CHUNK_WIDTH][CHUNK_HEIGHT][CHUNK_DEPTH];
        // In a real game, you might load data from a file or generator here.
    }

    // --- Getters for Coordinates ---
    public int getChunkX() { return chunkX; }
    public int getChunkY() { return chunkY; }
    public int getChunkZ() { return chunkZ; }


    /**
     * Sets the block type at the given local coordinates within the chunk.
     * Performs bounds checking. Thread-safety might be needed depending on usage.
     *
     * @param x Local X (0 to CHUNK_WIDTH - 1)
     * @param y Local Y (0 to CHUNK_HEIGHT - 1)
     * @param z Local Z (0 to CHUNK_DEPTH - 1)
     * @param blockType The new block type ID.
     * @return true if the block was set successfully, false if out of bounds.
     */
    public synchronized boolean setBlock(int x, int y, int z, byte blockType) {
        if (isOutOfBounds(x, y, z)) {
            // Logging might be better than System.err in common code
            // LogManager.getLogger().warn("SetBlock coordinates out of bounds...");
            return false;
        }
        blocks[x][y][z] = blockType;
        return true;
    }

    /**
     * Gets the block type at the given local coordinates.
     * Returns BLOCK_AIR if coordinates are out of bounds.
     *
     * @param x Local X (0 to CHUNK_WIDTH - 1)
     * @param y Local Y (0 to CHUNK_HEIGHT - 1)
     * @param z Local Z (0 to CHUNK_DEPTH - 1)
     * @return The block type ID, or BLOCK_AIR if out of bounds.
     */
    public byte getBlock(int x, int y, int z) {
        if (isOutOfBounds(x, y, z)) {
            return BLOCK_AIR;
        }
        return blocks[x][y][z];
    }

    /**
     * Checks if the local coordinates are outside the chunk's bounds [0, DIM-1].
     *
     * @param x Local X
     * @param y Local Y
     * @param z Local Z
     * @return true if coordinates are out of bounds, false otherwise.
     */
    public boolean isOutOfBounds(int x, int y, int z) {
        return x < 0 || x >= CHUNK_WIDTH ||
                y < 0 || y >= CHUNK_HEIGHT ||
                z < 0 || z >= CHUNK_DEPTH;
    }

    /**
     * Provides direct access to the underlying block data array.
     * Use with caution - intended for efficient iteration (e.g., meshing, saving).
     * Modifying the returned array directly bypasses bounds checks and synchronization.
     * @return The internal 3D byte array of block IDs.
     */
    public byte[][][] getBlocksRaw() {
        return blocks;
    }

    // Consider adding methods for serialization/deserialization here if needed by both S/C.
}