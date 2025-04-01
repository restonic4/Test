package com.chaotic_loom.game;

public class ChunkConstants {
    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 16;
    public static final int CHUNK_SIZE_Z = 16;

    // Derived constants
    public static final int CHUNK_AREA = CHUNK_SIZE_X * CHUNK_SIZE_Z;
    public static final int CHUNK_VOLUME = CHUNK_AREA * CHUNK_SIZE_Y;

    // Helper for index calculation
    public static int getIndex(int lx, int ly, int lz) {
        return lx + lz * CHUNK_SIZE_X + ly * CHUNK_AREA; // Order: X, then Z, then Y common for iteration
    }

    // Helper for local position validation
    public static boolean isValidLocalPos(int lx, int ly, int lz) {
        return lx >= 0 && lx < CHUNK_SIZE_X &&
                ly >= 0 && ly < CHUNK_SIZE_Y &&
                lz >= 0 && lz < CHUNK_SIZE_Z;
    }
}
