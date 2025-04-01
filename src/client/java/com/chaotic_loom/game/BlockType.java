package com.chaotic_loom.game;

public enum BlockType {
    // ID 0 is typically reserved for AIR
    AIR(false, true),   // Is Solid? Is Transparent?
    STONE(true, false),
    DIRT(true, false),
    GRASS(true, false), // Mesh generation logic might handle top/side textures differently
    GLASS(true, true);  // Still 'solid' for collision/meshing, but transparent for rendering logic
    // Add other block types here following the pattern

    private final boolean solid;
    private final boolean transparent; // For rendering face culling & potential transparency passes

    BlockType(boolean solid, boolean transparent) {
        this.solid = solid;
        this.transparent = transparent;
    }

    /**
     * @return True if the block should generally cull adjacent faces (like stone, dirt).
     */
    public boolean isSolid() {
        return solid;
    }

    /**
     * @return True if light passes through / rendering needs transparency handling (like glass, water).
     * Note: AIR is also considered transparent here for face culling logic.
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * Gets a simple byte ID based on the enum's declaration order.
     * Ensure AIR is the first entry (ordinal 0).
     * @return The byte ID.
     */
    public byte getID() {
        // Simple mapping based on enum ordinal. Explicit mapping might be safer
        // if order changes or gaps are needed, but ordinal is simple for now.
        return (byte) this.ordinal();
    }

    /**
     * Gets the BlockType corresponding to the given ID.
     * Defaults to AIR if the ID is out of bounds.
     * @param id The byte ID.
     * @return The corresponding BlockType, or AIR if invalid.
     */
    public static BlockType fromID(byte id) {
        BlockType[] values = values(); // Cache this if called extremely often
        if (id >= 0 && id < values.length) {
            return values[id];
        }
        return AIR; // Default to AIR for any invalid or out-of-range ID
    }
}