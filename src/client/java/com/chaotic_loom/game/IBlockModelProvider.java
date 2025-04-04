package com.chaotic_loom.game;

import com.chaotic_loom.game.rendering.components.ChunkMesher;
import com.chaotic_loom.game.world.components.Block;
import com.chaotic_loom.game.world.components.BlockInstance;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for defining how a block type renders its model,
 * handling different faces, rotations, and textures.
 */
public interface IBlockModelProvider {

    /**
     * Adds the geometry for a specific face of a block instance to the mesher's context.
     * Implementations should handle rotation based on blockInstance.getDirection(),
     * calculate correct texture coordinates using the atlas info obtained from
     * the context's TextureManager, and add vertices/uvs/normals/indices
     * to the appropriate lists (opaque or transparent) in the context.
     *
     * @param ctx The current mesh building context, providing access to geometry lists and TextureManager.
     * @param blockInstance The specific block instance being rendered (contains type and direction).
     * @param x The world X coordinate of the block (relative to chunk origin).
     * @param y The world Y coordinate of the block.
     * @param z The world Z coordinate of the block.
     * @param face The specific face of the block model that needs to be rendered (e.g., Block.Face.TOP).
     * @return true if the face was added successfully, false on critical error (e.g., missing fallback texture).
     */
    boolean addFaceGeometry(
            ChunkMesher.MeshBuildContext ctx, // Pass the context
            BlockInstance blockInstance,
            int x, int y, int z,
            Block.Face face
    );

    /**
     * Gets the appropriate Block.Face enum constant corresponding to the
     * geometric face defined by the neighbor offset used in ChunkMesher.
     * This is needed because the mesher checks neighbors using offsets,
     * but the model provider needs to know which logical face (TOP, FRONT, etc.)
     * corresponds to that check.
     *
     * Example: If the neighbor check is for (0, 1, 0) (block above), this
     * method should return Block.Face.TOP for a standard cube. Custom models
     * might map these differently.
     *
     * @param neighborDx Relative X offset of the neighbor (-1, 0, or 1)
     * @param neighborDy Relative Y offset of the neighbor (-1, 0, or 1)
     * @param neighborDz Relative Z offset of the neighbor (-1, 0, or 1)
     * @return The Block.Face corresponding to the neighbor direction, or null if invalid.
     */
    @Nullable
    Block.Face getFaceFromNeighborOffset(int neighborDx, int neighborDy, int neighborDz);
}