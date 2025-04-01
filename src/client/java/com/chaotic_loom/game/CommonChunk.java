package com.chaotic_loom.game;

import org.joml.Vector3i;
import org.joml.Vector3ic;

import static com.chaotic_loom.game.ChunkConstants.*;

public abstract class CommonChunk {

    protected final IChunkAccess worldAccess; // Access to parent world/entity for neighbor lookups
    protected final Vector3i chunkPos;      // Position in chunk coordinates relative to entity origin
    protected final byte[] blockIDs;        // Flattened 3D array storing BlockType IDs

    protected CommonChunk(IChunkAccess worldAccess, Vector3ic chunkPos) {
        this.worldAccess = worldAccess;
        this.chunkPos = new Vector3i(chunkPos);
        this.blockIDs = new byte[CHUNK_VOLUME]; // Defaults to 0 (AIR)
    }

    public Vector3ic getChunkPos() {
        return chunkPos;
    }

    /**
     * Gets the block ID strictly within this chunk's bounds.
     * Returns AIR ID if coords are invalid.
     */
    public byte getBlockIDLocal(int lx, int ly, int lz) {
        if (isValidLocalPos(lx, ly, lz)) {
            return blockIDs[getIndex(lx, ly, lz)];
        }
        return BlockType.AIR.getID(); // Outside local bounds is Air from this chunk's perspective
    }

    /**
     * Gets the block ID at the given local coords, querying neighbours via worldAccess if needed.
     * This is typically used by meshing or logic needing neighbor info.
     */
    public byte getBlockIDWithNeighbors(int lx, int ly, int lz) {
        if (isValidLocalPos(lx, ly, lz)) {
            return blockIDs[getIndex(lx, ly, lz)];
        }
        // If coords are outside, ask the world/entity access interface
        Vector3i worldBlockPos = localToWorldBlockPos(lx, ly, lz, new Vector3i());
        // Assuming worldAccess handles looking up the correct block type even if chunk not loaded yet
        return worldAccess.getBlockType(worldBlockPos).getID();
    }

    /** Gets the BlockType at the given local coords, querying neighbours via worldAccess if needed. */
    public BlockType getBlockTypeWithNeighbors(int lx, int ly, int lz) {
        // Can be optimized by not converting back and forth if getBlockIDWithNeighbors is called first
        if (isValidLocalPos(lx, ly, lz)) {
            return BlockType.fromID(blockIDs[getIndex(lx, ly, lz)]);
        }
        Vector3i worldBlockPos = localToWorldBlockPos(lx, ly, lz, new Vector3i());
        return worldAccess.getBlockType(worldBlockPos);
    }

    /**
     * Sets the block ID at local chunk coordinates. Marks this chunk dirty
     * and notifies worldAccess to mark neighbors dirty if on a border.
     * Assumes the coordinates are valid local coordinates (0-15).
     * External code should use BlockGridEntity.setBlockID for world coordinates.
     */
    public void setBlockIDLocal(int lx, int ly, int lz, byte id) {
        if (!isValidLocalPos(lx, ly, lz)) {
            // Should not happen if called correctly
            System.err.printf("Invalid local coords in setBlockIDLocal: (%d, %d, %d)%n", lx, ly, lz);
            return;
        }
        int index = getIndex(lx, ly, lz);
        if (blockIDs[index] != id) { // Only process if block actually changed
            blockIDs[index] = id;
            markDirty(); // Mark this chunk dirty (implemented in subclasses)

            // Mark neighbors as dirty if on a border
            if (lx == 0) worldAccess.markNeighborDirty(chunkPos, -1, 0, 0);
            if (lx == CHUNK_SIZE_X - 1) worldAccess.markNeighborDirty(chunkPos, 1, 0, 0);
            if (ly == 0) worldAccess.markNeighborDirty(chunkPos, 0, -1, 0);
            if (ly == CHUNK_SIZE_Y - 1) worldAccess.markNeighborDirty(chunkPos, 0, 1, 0);
            if (lz == 0) worldAccess.markNeighborDirty(chunkPos, 0, 0, -1);
            if (lz == CHUNK_SIZE_Z - 1) worldAccess.markNeighborDirty(chunkPos, 0, 0, 1);
        }
    }

    /** Sets the BlockType at local chunk coordinates. */
    public void setBlockTypeLocal(int lx, int ly, int lz, BlockType type) {
        setBlockIDLocal(lx, ly, lz, type.getID());
    }

    /** Converts local chunk coordinates to world block coordinates relative to the entity origin. */
    public Vector3i localToWorldBlockPos(int lx, int ly, int lz, Vector3i dest) {
        return dest.set(chunkPos.x * CHUNK_SIZE_X + lx,
                chunkPos.y * CHUNK_SIZE_Y + ly,
                chunkPos.z * CHUNK_SIZE_Z + lz);
    }

    // Abstract methods to be implemented by ClientChunk/ServerChunk
    /** Marks this chunk as needing an update (e.g., mesh regeneration or collision shape recalc). */
    public abstract void markDirty();

    /** Performs any per-tick updates needed for this chunk (server-side mainly). */
    public abstract void updateTick();

    /** Cleans up any resources held by this chunk (e.g., Mesh on client). */
    public abstract void cleanup();

}