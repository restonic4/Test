package com.chaotic_loom.game.world;

import com.chaotic_loom.game.core.Loggers;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.built_in.Blocks;
import com.chaotic_loom.game.world.components.Block;
import com.chaotic_loom.game.world.components.BlockInstance;

import static com.chaotic_loom.game.core.util.SharedConstants.*;

public class ChunkData {
    // Core data: 3D array of block volatile IDs.
    private final BlockInstance[][][] blocks;

    // Store chunk's position in the world grid
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

        this.blocks = new BlockInstance[CHUNK_WIDTH][CHUNK_HEIGHT][CHUNK_DEPTH];

        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_DEPTH; z++) {
                    this.blocks[x][y][z] = Blocks.AIR.createInstance();
                }
            }
        }
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
     * @param block The new block.
     * @return true if the block was set successfully, false if out of bounds.
     */
    public synchronized boolean setBlock(int x, int y, int z, BlockInstance block) {
        if (isOutOfBounds(x, y, z)) {
            Loggers.CHUNK.error("SetBlock coordinates out of bounds...");
            return false;
        }

        blocks[x][y][z] = block;
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
    public BlockInstance getBlock(int x, int y, int z) {
        BlockInstance blockInstance;

        if (isOutOfBounds(x, y, z)) {
            blockInstance = Blocks.AIR.createInstance();
        } else {
            blockInstance =  blocks[x][y][z];
        }

        return blockInstance;
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
    public BlockInstance[][][] getBlocksRaw() {
        return blocks;
    }
}