package com.chaotic_loom.game;

public final class BlockTypes {
    private BlockTypes() {} // Prevent instantiation

    // Chunk Dimensions (Shared)
    public static final int CHUNK_WIDTH = 16;
    public static final int CHUNK_HEIGHT = 16;
    public static final int CHUNK_DEPTH = 16;

    // Block Type IDs
    public static final byte BLOCK_AIR = 0;
    public static final byte BLOCK_DIRT = 1;
    public static final byte BLOCK_STONE = 2;
    public static final byte BLOCK_GRASS = 3;
    public static final byte BLOCK_WOOD_LOG = 4;
    public static final byte BLOCK_GLASS = 5;
    // Add more block types as needed...

    /**
     * Checks if a given block type is considered fully opaque.
     * This is common logic needed by both client (meshing) and server (potentially pathfinding, visibility).
     *
     * @param blockType The block type ID.
     * @return true if the block type is opaque, false otherwise.
     */
    public static boolean isBlockOpaque(byte blockType) {
        switch (blockType) {
            case BLOCK_AIR:
            case BLOCK_GLASS: // Glass is not opaque
                return false;
            // Add cases for other non-opaque blocks (water, leaves if using alpha, etc.)
            case BLOCK_DIRT:
            case BLOCK_STONE:
            case BLOCK_GRASS:
            case BLOCK_WOOD_LOG:
            default: // Assume undefined blocks are opaque (or handle explicitly)
                return true; // Check if blockType is known?
        }
    }
}