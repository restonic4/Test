package com.chaotic_loom.game;

import com.chaotic_loom.game.core.Loggers;
import com.chaotic_loom.game.rendering.TextureManager;
import com.chaotic_loom.game.rendering.components.ChunkMesher;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;
import com.chaotic_loom.game.world.components.Block;
import com.chaotic_loom.game.world.components.BlockInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StairsModelProvider implements IBlockModelProvider {

    // --- Static Geometry Definition (Base Model: Facing NORTH, steps ascend towards -Z) ---

    private enum StairPart {
        BOTTOM, TOP_LOWER, TOP_UPPER, BACK,
        FRONT_LOWER, FRONT_UPPER_RISER,
        SIDE_LEFT_LOWER, SIDE_LEFT_UPPER,
        SIDE_RIGHT_LOWER, SIDE_RIGHT_UPPER
    }

    // --- Geometry Data ---
    // Note: Vertex order is generally Bottom-Left, Bottom-Right, Top-Right, Top-Left (for standard UV mapping)

    // -Y Face
    private static final float[] POS_BOTTOM = { -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f };
    private static final float[] NORM_BOTTOM = { 0,-1,0,  0,-1,0,  0,-1,0,  0,-1,0 };
    private static final float[] UV_BOTTOM = { 0,1,  1,1,  1,0,  0,0 }; // Full texture

    // +Y Faces
    private static final float[] POS_TOP_LOWER = { -0.5f, 0.0f,-0.5f,  0.5f, 0.0f,-0.5f,  0.5f, 0.0f, 0.0f, -0.5f, 0.0f, 0.0f }; // Front half top
    private static final float[] NORM_TOP_LOWER = { 0,1,0,  0,1,0,  0,1,0,  0,1,0 };
    private static final float[] UV_TOP_LOWER = { 0,0,  1,0,  1,0.5f,  0,0.5f }; // Use lower half of texture

    private static final float[] POS_TOP_UPPER = { -0.5f, 0.5f, 0.0f,  0.5f, 0.5f, 0.0f,  0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f }; // Back half top
    private static final float[] NORM_TOP_UPPER = { 0,1,0,  0,1,0,  0,1,0,  0,1,0 };
    private static final float[] UV_TOP_UPPER = { 0,0.5f,  1,0.5f,  1,1,  0,1 }; // Use upper half of texture

    // +Z Face
    private static final float[] POS_BACK = { -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f,  0.5f, 0.5f, 0.5f,  0.5f,-0.5f, 0.5f };
    private static final float[] NORM_BACK = { 0,0,1,  0,0,1,  0,0,1,  0,0,1 };
    private static final float[] UV_BACK = { 1,1,  1,0,  0,0,  0,1 }; // Flipped U for standard back face

    // -Z Faces
    private static final float[] POS_FRONT_LOWER = { -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,  0.5f, 0.0f,-0.5f, -0.5f, 0.0f,-0.5f }; // Front face, lower half
    private static final float[] NORM_FRONT_LOWER = { 0,0,-1,  0,0,-1,  0,0,-1,  0,0,-1 };
    private static final float[] UV_FRONT_LOWER = { 0,1,  1,1,  1,0.5f,  0,0.5f }; // Lower half of texture

    private static final float[] POS_FRONT_UPPER_RISER = { -0.5f, 0.0f, 0.0f,  0.5f, 0.0f, 0.0f,  0.5f, 0.5f, 0.0f, -0.5f, 0.5f, 0.0f }; // Riser face
    private static final float[] NORM_FRONT_UPPER_RISER = { 0,0,-1,  0,0,-1,  0,0,-1,  0,0,-1 }; // Still faces -Z
    private static final float[] UV_FRONT_UPPER_RISER = { 0,0.5f,  1,0.5f,  1,0,  0,0 }; // Upper half of texture (flipped V)

    // -X Faces (Left Side)
    private static final float[] POS_SIDE_LEFT_LOWER = { -0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f,-0.5f }; // Lower part, Z goes from front to back, Y bottom to mid
    private static final float[] NORM_SIDE_LEFT_LOWER = { -1,0,0,  -1,0,0,  -1,0,0,  -1,0,0 };
    // UV Mapping for combined sides can be tricky. Let's map Z to U and Y to V.
    private static final float[] UV_SIDE_LEFT_LOWER = { 0,1,  1,1,  1,0.5f,  0,0.5f }; // Full Z range, lower Y half

    private static final float[] POS_SIDE_LEFT_UPPER = { -0.5f, 0.0f, 0.0f, -0.5f, 0.0f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.0f }; // Upper part, Z goes from mid to back, Y mid to top
    private static final float[] NORM_SIDE_LEFT_UPPER = { -1,0,0,  -1,0,0,  -1,0,0,  -1,0,0 };
    private static final float[] UV_SIDE_LEFT_UPPER = { 0.5f,0.5f,  1,0.5f,  1,0,  0.5f,0 }; // Back Z half, upper Y half

    // +X Faces (Right Side)
    private static final float[] POS_SIDE_RIGHT_LOWER = { 0.5f,-0.5f,-0.5f, 0.5f, 0.0f,-0.5f, 0.5f, 0.0f, 0.5f, 0.5f,-0.5f, 0.5f }; // Lower part, Z front to back, Y bottom to mid
    private static final float[] NORM_SIDE_RIGHT_LOWER = { 1,0,0,  1,0,0,  1,0,0,  1,0,0 };
    private static final float[] UV_SIDE_RIGHT_LOWER = { 0,1,  0,0.5f,  1,0.5f,  1,1 }; // Full Z range, lower Y half (U flipped vs left)

    private static final float[] POS_SIDE_RIGHT_UPPER = { 0.5f, 0.0f, 0.0f, 0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f, 0.0f, 0.5f }; // Upper part, Z mid to back, Y mid to top
    private static final float[] NORM_SIDE_RIGHT_UPPER = { 1,0,0,  1,0,0,  1,0,0,  1,0,0 };
    private static final float[] UV_SIDE_RIGHT_UPPER = { 0.5f,0.5f,  0.5f,0,  1,0,  1,0.5f }; // Back Z half, upper Y half (U flipped vs left)

    // --- End of Static Geometry Definition ---


    // Singleton instance
    public static final StairsModelProvider INSTANCE = new StairsModelProvider();

    private StairsModelProvider() {}

    // Rotations for horizontal directions
    private static final Map<Block.Direction, Quaternionf> ROTATIONS = Map.of(
            Block.Direction.NORTH, new Quaternionf(), // Base model faces North, no rotation needed
            Block.Direction.SOUTH, new Quaternionf().rotateY((float) Math.toRadians(180)),
            Block.Direction.WEST, new Quaternionf().rotateY((float) Math.toRadians(90)),
            Block.Direction.EAST, new Quaternionf().rotateY((float) Math.toRadians(-90))
    );

    @Override
    public boolean addFaceGeometry(ChunkMesher.MeshBuildContext ctx, BlockInstance blockInstance, int x, int y, int z, Block.Face face) {
        Block block = blockInstance.getBlock();
        Block.Direction direction = blockInstance.getDirection();

        if (!direction.isHorizontal()) {
            direction = Block.Direction.NORTH; // Default non-horizontal
        }

        Quaternionf rotation = ROTATIONS.get(direction);
        List<StairPart> partsToAdd = getPartsForFace(face, direction);

        if (partsToAdd.isEmpty()) {
            return true; // Nothing to add for this face/direction combo
        }

        boolean success = true;
        for (StairPart part : partsToAdd) {
            success &= addPartGeometry(ctx, blockInstance, x, y, z, rotation, part, face);
        }

        return success;
    }

    private List<StairPart> getPartsForFace(Block.Face logicalFace, Block.Direction stairDirection) {
        List<StairPart> parts = new ArrayList<>();
        // This logic maps the *world-relative* logical face requested by the mesher
        // back to the *model-relative* parts of our base North-facing stair model.
        switch (stairDirection) {
            case NORTH: // Base orientation
                switch (logicalFace) {
                    case TOP:    parts.add(StairPart.TOP_LOWER); parts.add(StairPart.TOP_UPPER); break;
                    case BOTTOM: parts.add(StairPart.BOTTOM); break;
                    case FRONT:  parts.add(StairPart.FRONT_LOWER); parts.add(StairPart.FRONT_UPPER_RISER); break; // Front is -Z
                    case BACK:   parts.add(StairPart.BACK); break; // Back is +Z
                    case LEFT:   parts.add(StairPart.SIDE_LEFT_LOWER); parts.add(StairPart.SIDE_LEFT_UPPER); break; // Left is -X
                    case RIGHT:  parts.add(StairPart.SIDE_RIGHT_LOWER); parts.add(StairPart.SIDE_RIGHT_UPPER); break; // Right is +X
                }
                break;
            case SOUTH: // Rotated 180 degrees
                switch (logicalFace) {
                    case TOP:    parts.add(StairPart.TOP_LOWER); parts.add(StairPart.TOP_UPPER); break;
                    case BOTTOM: parts.add(StairPart.BOTTOM); break;
                    case FRONT:  parts.add(StairPart.BACK); break; // World Front (+Z) is Model Back
                    case BACK:   parts.add(StairPart.FRONT_LOWER); parts.add(StairPart.FRONT_UPPER_RISER); break; // World Back (-Z) is Model Front
                    case LEFT:   parts.add(StairPart.SIDE_RIGHT_LOWER); parts.add(StairPart.SIDE_RIGHT_UPPER); break; // World Left (-X) is Model Right
                    case RIGHT:  parts.add(StairPart.SIDE_LEFT_LOWER); parts.add(StairPart.SIDE_LEFT_UPPER); break; // World Right (+X) is Model Left
                }
                break;
            case EAST: // Rotated -90 degrees around Y (Model +X points World +Z, Model +Z points World -X)
                switch (logicalFace) {
                    case TOP:    parts.add(StairPart.TOP_LOWER); parts.add(StairPart.TOP_UPPER); break;
                    case BOTTOM: parts.add(StairPart.BOTTOM); break;
                    case FRONT:  parts.add(StairPart.SIDE_LEFT_LOWER); parts.add(StairPart.SIDE_LEFT_UPPER); break; // World Front (+Z) is Model Left (-X)
                    case BACK:   parts.add(StairPart.SIDE_RIGHT_LOWER); parts.add(StairPart.SIDE_RIGHT_UPPER); break; // World Back (-Z) is Model Right (+X)
                    case LEFT:   parts.add(StairPart.BACK); break; // World Left (-X) is Model Back (+Z)
                    case RIGHT:  parts.add(StairPart.FRONT_LOWER); parts.add(StairPart.FRONT_UPPER_RISER); break; // World Right (+X) is Model Front (-Z)
                }
                break;
            case WEST: // Rotated 90 degrees around Y (Model +X points World -Z, Model +Z points World +X)
                switch (logicalFace) {
                    case TOP:    parts.add(StairPart.TOP_LOWER); parts.add(StairPart.TOP_UPPER); break;
                    case BOTTOM: parts.add(StairPart.BOTTOM); break;
                    case FRONT:  parts.add(StairPart.SIDE_RIGHT_LOWER); parts.add(StairPart.SIDE_RIGHT_UPPER); break; // World Front (+Z) is Model Right (+X)
                    case BACK:   parts.add(StairPart.SIDE_LEFT_LOWER); parts.add(StairPart.SIDE_LEFT_UPPER); break; // World Back (-Z) is Model Left (-X)
                    case LEFT:   parts.add(StairPart.FRONT_LOWER); parts.add(StairPart.FRONT_UPPER_RISER); break; // World Left (-X) is Model Front (-Z)
                    case RIGHT:  parts.add(StairPart.BACK); break; // World Right (+X) is Model Back (+Z)
                }
                break;
        }
        return parts;
    }

    // Adds the geometry for a single StairPart
    private boolean addPartGeometry(ChunkMesher.MeshBuildContext ctx, BlockInstance blockInstance,
                                    int x, int y, int z, Quaternionf rotation,
                                    StairPart part, Block.Face textureFace) {

        Block block = blockInstance.getBlock();
        boolean isOpaque = !block.getSettings().isTransparent();

        // --- 1. Get Texture Atlas Info ---
        TextureAtlasInfo atlasInfo = getTextureAtlasInfoForBlockFace(ctx.textureManager, block, textureFace);
        if (atlasInfo == null) {
            Loggers.RENDERER.error("StairsModelProvider: Missing TextureAtlasInfo for block %s, logical face %s (part %s) at [%d,%d,%d]. Using fallback.%n",
                    block.getIdentifier(), textureFace, part, x, y, z);
            atlasInfo = ctx.textureManager.getTextureInfo("/textures/debug_missing.png");
            if (atlasInfo == null) {
                Loggers.RENDERER.error("StairsModelProvider: FATAL - Fallback texture missing!");
                return false;
            }
        }

        // --- 2. Verify Atlas Texture ---
        if (ctx.atlasTexture == null) ctx.atlasTexture = atlasInfo.atlasTexture();
        else if (ctx.atlasTexture != atlasInfo.atlasTexture()) {
            Loggers.RENDERER.error("StairsModelProvider: Multiple atlases detected!"); return false;
        }

        // --- 3. Get Geometry Data for the Part ---
        float[] positions = getPartPositions(part);
        float[] normals = getPartNormals(part);
        float[] baseUVs = getPartUVs(part);

        // Check if data was actually retrieved (it should be now)
        if (positions == null || normals == null || baseUVs == null) {
            Loggers.RENDERER.error("StairsModelProvider: CRITICAL - Geometry data arrays are STILL null for part: {}", part);
            return false; // Hard fail if data is missing now
        }

        // --- 4. Determine target lists and vertex offset ---
        List<Float> targetPositions;
        List<Float> targetUvs;
        List<Float> targetNormals;
        List<Integer> targetIndices;
        int baseVertexIndex;

        // Assuming stairs are always opaque
        targetPositions = ctx.positions_opaque;
        targetUvs = ctx.uvs_opaque;
        targetNormals = ctx.normals_opaque;
        targetIndices = ctx.indices_opaque;
        baseVertexIndex = ctx.currentVertexOffset_opaque;

        // --- 5. Add Rotated Vertices, UVs, Normals for this part ---
        Vector4f pos = new Vector4f(0, 0, 0, 1);
        Vector3f norm = new Vector3f();

        for (int i = 0; i < 4; i++) {
            pos.set(positions[i*3], positions[i*3+1], positions[i*3+2], 1.0f);
            norm.set(normals[i*3], normals[i*3+1], normals[i*3+2]);

            rotation.transform(pos);
            rotation.transform(norm);
            norm.normalize(); // Ensure unit length

            targetPositions.add(pos.x + x);
            targetPositions.add(pos.y + y);
            targetPositions.add(pos.z + z);

            targetNormals.add(norm.x);
            targetNormals.add(norm.y);
            targetNormals.add(norm.z);

            float baseU = baseUVs[i*2];
            float baseV = baseUVs[i*2+1];
            float finalU = atlasInfo.u0() + baseU * atlasInfo.getWidthUV();
            float finalV = atlasInfo.v0() + baseV * atlasInfo.getHeightUV();
            targetUvs.add(finalU);
            targetUvs.add(finalV);
        }

        // --- 6. Add Indices for this part's quad ---
        targetIndices.add(baseVertexIndex + 0);
        targetIndices.add(baseVertexIndex + 1);
        targetIndices.add(baseVertexIndex + 2);
        targetIndices.add(baseVertexIndex + 0);
        targetIndices.add(baseVertexIndex + 2);
        targetIndices.add(baseVertexIndex + 3);

        // --- 7. Increment offset for the next part/face ---
        if(isOpaque) {
            ctx.currentVertexOffset_opaque += 4;
        } else {
            // ctx.currentVertexOffset_transparent += 4; // If transparent stairs exist
        }
        return true;
    }

    // Helper methods to retrieve geometry data
    private float[] getPartPositions(StairPart part) {
        switch (part) {
            case BOTTOM: return POS_BOTTOM;
            case TOP_LOWER: return POS_TOP_LOWER;
            case TOP_UPPER: return POS_TOP_UPPER;
            case BACK: return POS_BACK;
            case FRONT_LOWER: return POS_FRONT_LOWER;
            case FRONT_UPPER_RISER: return POS_FRONT_UPPER_RISER;
            case SIDE_LEFT_LOWER: return POS_SIDE_LEFT_LOWER;
            case SIDE_LEFT_UPPER: return POS_SIDE_LEFT_UPPER;
            case SIDE_RIGHT_LOWER: return POS_SIDE_RIGHT_LOWER;
            case SIDE_RIGHT_UPPER: return POS_SIDE_RIGHT_UPPER;
            default: return null; // Should not happen
        }
    }
    private float[] getPartNormals(StairPart part) {
        switch (part) {
            case BOTTOM: return NORM_BOTTOM;
            case TOP_LOWER: return NORM_TOP_LOWER;
            case TOP_UPPER: return NORM_TOP_UPPER;
            case BACK: return NORM_BACK;
            case FRONT_LOWER: return NORM_FRONT_LOWER;
            case FRONT_UPPER_RISER: return NORM_FRONT_UPPER_RISER;
            case SIDE_LEFT_LOWER: return NORM_SIDE_LEFT_LOWER;
            case SIDE_LEFT_UPPER: return NORM_SIDE_LEFT_UPPER;
            case SIDE_RIGHT_LOWER: return NORM_SIDE_RIGHT_LOWER;
            case SIDE_RIGHT_UPPER: return NORM_SIDE_RIGHT_UPPER;
            default: return null;
        }
    }
    private float[] getPartUVs(StairPart part) {
        switch (part) {
            case BOTTOM: return UV_BOTTOM;
            case TOP_LOWER: return UV_TOP_LOWER;
            case TOP_UPPER: return UV_TOP_UPPER;
            case BACK: return UV_BACK;
            case FRONT_LOWER: return UV_FRONT_LOWER;
            case FRONT_UPPER_RISER: return UV_FRONT_UPPER_RISER;
            case SIDE_LEFT_LOWER: return UV_SIDE_LEFT_LOWER;
            case SIDE_LEFT_UPPER: return UV_SIDE_LEFT_UPPER;
            case SIDE_RIGHT_LOWER: return UV_SIDE_RIGHT_LOWER;
            case SIDE_RIGHT_UPPER: return UV_SIDE_RIGHT_UPPER;
            default: return new float[]{0,0, 1,0, 1,1, 0,1}; // Fallback UVs
        }
    }

    // Get Texture Atlas Info
    private TextureAtlasInfo getTextureAtlasInfoForBlockFace(TextureManager textureManager, Block block, Block.Face face) {
        Map<Block.Face, String> textures = block.getSettings().getFaceProperties().getTextures();
        String texturePath = textures.get(face);

        // Simple fallback: If a specific face (like FRONT) isn't defined, maybe use the texture defined for BACK or SIDE?
        // A more robust solution involves dedicated 'side' textures in Block.Settings or more complex mapping.
        if (texturePath == null) {
            // Example: try SIDE texture if FRONT/BACK/LEFT/RIGHT missing
            // This requires you to define how Block.Face maps to potential texture keys
            // For now, just log and return null (leading to fallback texture)
            // System.err.printf("StairsModelProvider: Texture path is null for block %s, face %s. Consider fallback logic.%n", block.getIdentifier(), face);
            return null; // Will trigger fallback in addPartGeometry
        }
        return textureManager.getTextureInfo(texturePath);
    }

    @Override
    @Nullable
    public Block.Face getFaceFromNeighborOffset(int neighborDx, int neighborDy, int neighborDz) {
        // World-relative face determination - same as Cube
        if (neighborDx == 1 && neighborDy == 0 && neighborDz == 0) return Block.Face.RIGHT;
        if (neighborDx == -1 && neighborDy == 0 && neighborDz == 0) return Block.Face.LEFT;
        if (neighborDx == 0 && neighborDy == 1 && neighborDz == 0) return Block.Face.TOP;
        if (neighborDx == 0 && neighborDy == -1 && neighborDz == 0) return Block.Face.BOTTOM;
        if (neighborDx == 0 && neighborDy == 0 && neighborDz == 1) return Block.Face.FRONT;
        if (neighborDx == 0 && neighborDy == 0 && neighborDz == -1) return Block.Face.BACK;
        return null;
    }
}