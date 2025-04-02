package com.chaotic_loom.game;

import com.chaotic_loom.game.rendering.TextureManager;
import com.chaotic_loom.game.rendering.mesh.Cube;
import com.chaotic_loom.game.rendering.mesh.Mesh;
import com.chaotic_loom.game.rendering.texture.Texture;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ChunkMesher {

    private ChunkMesher() {} // Static class

    /**
     * Result holder for mesh generation.
     */
    public record ChunkMeshBuildResult(
            @Nullable Mesh meshOpaque,
            @Nullable Mesh meshTransparent,
            @Nullable Texture atlasTexture
    ) {}

    // Internal context class to hold state during mesh generation for one chunk
    private static class MeshBuildContext {
        final ChunkData chunkData;
        final TextureManager textureManager;
        // TODO: Need access to neighbor chunks for seamless meshing across boundaries
        Texture atlasTexture = null;

        // Geometry Lists
        final List<Float> positions_opaque = new ArrayList<>();
        final List<Float> uvs_opaque = new ArrayList<>();
        final List<Float> normals_opaque = new ArrayList<>();
        final List<Integer> indices_opaque = new ArrayList<>();
        int currentVertexOffset_opaque = 0;

        final List<Float> positions_transparent = new ArrayList<>();
        final List<Float> uvs_transparent = new ArrayList<>();
        final List<Float> normals_transparent = new ArrayList<>();
        final List<Integer> indices_transparent = new ArrayList<>();
        int currentVertexOffset_transparent = 0;

        MeshBuildContext(ChunkData chunkData, TextureManager textureManager) {
            this.chunkData = chunkData;
            this.textureManager = textureManager;
        }
    }

    /**
     * Generates opaque and transparent meshes for the given chunk data.
     * @param chunkData The data to mesh.
     * @param textureManager The texture manager to look up atlas info.
     * @return A ChunkMeshBuildResult containing the generated meshes (or nulls) and atlas texture.
     */
    public static ChunkMeshBuildResult generateMeshes(ChunkData chunkData, TextureManager textureManager /*, WorldAccessor world */) {

        MeshBuildContext ctx = new MeshBuildContext(chunkData, textureManager /*, world */);

        // Iterate through blocks within the chunk
        for (int x = 0; x < BlockTypes.CHUNK_WIDTH; x++) {
            for (int y = 0; y < BlockTypes.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < BlockTypes.CHUNK_DEPTH; z++) {

                    byte currentBlockType = ctx.chunkData.getBlock(x, y, z);
                    if (currentBlockType == BlockTypes.BLOCK_AIR) continue;

                    boolean currentIsOpaque = BlockTypes.isBlockOpaque(currentBlockType);

                    // Define neighbour RELATIVE offsets for cleaner loop
                    int[][] neighborOffsets = {
                            { 0,  0,  1, 0}, { 0,  0, -1, 1}, { 0,  1,  0, 2},
                            { 0, -1,  0, 3}, { 1,  0,  0, 4}, {-1,  0,  0, 5}
                    };

                    for (int[] offset : neighborOffsets) {
                        int nx = x + offset[0];
                        int ny = y + offset[1];
                        int nz = z + offset[2];
                        int faceIndex = offset[3];

                        // Use chunkData's getBlock to handle boundary checks simply (returns AIR if out)
                        // For seamless meshing, a WorldAccessor would be needed here to query actual neighbor chunks.
                        byte neighborBlockType = ctx.chunkData.getBlock(nx, ny, nz);
                        boolean neighborIsOpaque = BlockTypes.isBlockOpaque(neighborBlockType);

                        // Visibility Rule (Occlusion check reverted as requested)
                        boolean shouldRenderFace = false;
                        if (currentIsOpaque && !neighborIsOpaque) {
                            shouldRenderFace = true;
                        } else if (!currentIsOpaque && neighborBlockType == BlockTypes.BLOCK_AIR) {
                            shouldRenderFace = true;
                        }

                        if (shouldRenderFace) {
                            boolean success = addFace(ctx, currentIsOpaque, x, y, z, currentBlockType, faceIndex);
                            if (!success) {
                                System.err.println("ChunkMesher: Failed during addFace at " + x + "," + y + "," + z + ". Aborting mesh generation for this chunk.");
                                return null; // Critical error
                            }
                        }
                    }
                }
            }
        }

        // Build Mesh Objects from context lists
        Mesh meshOpaque = buildMeshFromContext(ctx.positions_opaque, ctx.uvs_opaque, ctx.normals_opaque, ctx.indices_opaque);
        Mesh meshTransparent = buildMeshFromContext(ctx.positions_transparent, ctx.uvs_transparent, ctx.normals_transparent, ctx.indices_transparent);

        if (meshOpaque == null && meshTransparent == null) {
            // System.out.println("ChunkMesher: Chunk resulted in no geometry."); // Optional: Logging
            return null;
        }

        return new ChunkMeshBuildResult(meshOpaque, meshTransparent, ctx.atlasTexture);
    }

    @Nullable
    private static Mesh buildMeshFromContext(List<Float> positions, List<Float> uvs, List<Float> normals, List<Integer> indices) {
        if (positions.isEmpty()) {
            return null;
        }
        float[] posArray = toFloatArray(positions);
        float[] uvArray = toFloatArray(uvs);
        float[] normArray = toFloatArray(normals);
        int[] idxArray = toIntArray(indices);
        // Keep the '100' placeholder as requested
        return new Mesh(posArray, uvArray, normArray, idxArray, 100);
    }

    /**
     * Adds the vertex data for a single face to the appropriate lists in the build context.
     */
    private static boolean addFace(MeshBuildContext ctx, boolean isOpaqueFace, int x, int y, int z, byte blockType, int faceIndex) {
        // 1. Get Texture Atlas Info
        TextureAtlasInfo atlasInfo = getTextureAtlasInfoForBlock(ctx, blockType, faceIndex);
        if (atlasInfo == null) {
            // Attempt to use fallback texture
            System.err.printf("ChunkMesher: Missing TextureAtlasInfo for block %d, face %d at [%d,%d,%d]. Using fallback.%n", blockType, faceIndex, x, y, z);
            atlasInfo = ctx.textureManager.getTextureInfo("/textures/debug_missing.png");
            if (atlasInfo == null) {
                System.err.println("ChunkMesher: FATAL - Fallback texture '/textures/debug_missing.png' not found in TextureManager!");
                return false; // Critical if fallback is missing
            }
        }

        // 2. Store/Verify Atlas Texture (Ensures all geometry in this result uses one atlas)
        if (ctx.atlasTexture == null) {
            ctx.atlasTexture = atlasInfo.atlasTexture();
        } else if (ctx.atlasTexture != atlasInfo.atlasTexture()) {
            // This indicates a setup error - block textures are spread across multiple atlases,
            // which this simple mesher doesn't support in a single pass.
            System.err.println("ChunkMesher: CRITICAL ERROR - Encountered multiple texture atlases during mesh generation! Cannot proceed.");
            return false;
        }

        // 3. Calculate start indices within Cube's static arrays
        // (Cube provides vertices for a unit cube centered at origin)
        int posStartIndex = faceIndex * 12; // faceIndex * 4 vertices * 3 floats/pos
        int uvStartIndex = faceIndex * 8;   // faceIndex * 4 vertices * 2 floats/uv
        int normStartIndex = faceIndex * 12; // faceIndex * 4 vertices * 3 floats/normal

        // 4. Determine target lists and vertex offset from context 'ctx'
        List<Float> targetPositions;
        List<Float> targetUvs;
        List<Float> targetNormals;
        List<Integer> targetIndices;
        int baseVertexIndex;

        if (isOpaqueFace) {
            targetPositions = ctx.positions_opaque;
            targetUvs = ctx.uvs_opaque;
            targetNormals = ctx.normals_opaque;
            targetIndices = ctx.indices_opaque;
            baseVertexIndex = ctx.currentVertexOffset_opaque;
            ctx.currentVertexOffset_opaque += 4; // Increment for the next opaque face
        } else {
            targetPositions = ctx.positions_transparent;
            targetUvs = ctx.uvs_transparent;
            targetNormals = ctx.normals_transparent;
            targetIndices = ctx.indices_transparent;
            baseVertexIndex = ctx.currentVertexOffset_transparent;
            ctx.currentVertexOffset_transparent += 4; // Increment for the next transparent face
        }

        // 5. Add the 4 vertices for this face to the TARGET lists
        for (int i = 0; i < 4; i++) {
            // Position: Get base vertex relative to (0,0,0) and offset by block's position (x, y, z)
            targetPositions.add(Cube.POSITIONS[posStartIndex + i * 3 + 0] + x);
            targetPositions.add(Cube.POSITIONS[posStartIndex + i * 3 + 1] + y);
            targetPositions.add(Cube.POSITIONS[posStartIndex + i * 3 + 2] + z);

            // UVs: Get base UV (0-1 range) and map it to the atlas sub-region
            float baseU = Cube.BASE_UVS[uvStartIndex + i * 2 + 0];
            float baseV = Cube.BASE_UVS[uvStartIndex + i * 2 + 1];
            float finalU = atlasInfo.u0() + baseU * atlasInfo.getWidthUV();
            float finalV = atlasInfo.v0() + baseV * atlasInfo.getHeightUV();
            targetUvs.add(finalU);
            targetUvs.add(finalV);

            // Normals: Use the base normal for the face
            targetNormals.add(Cube.NORMALS[normStartIndex + i * 3 + 0]);
            targetNormals.add(Cube.NORMALS[normStartIndex + i * 3 + 1]);
            targetNormals.add(Cube.NORMALS[normStartIndex + i * 3 + 2]);
        }

        // 6. Add the 6 indices to the TARGET list (forming two triangles for the quad)
        targetIndices.add(baseVertexIndex + 0);
        targetIndices.add(baseVertexIndex + 1);
        targetIndices.add(baseVertexIndex + 2);
        targetIndices.add(baseVertexIndex + 0);
        targetIndices.add(baseVertexIndex + 2);
        targetIndices.add(baseVertexIndex + 3);

        return true; // Success
    }

    /**
     * Looks up the TextureAtlasInfo using the TextureManager based on block type and face.
     */
    private static TextureAtlasInfo getTextureAtlasInfoForBlock(MeshBuildContext ctx, byte blockType, int faceIndex) {
        String texturePath = getTexturePathForBlockFace(blockType, faceIndex);
        if (texturePath == null) {
            // Logged within addFace now if fallback is used
            return null;
        }
        // TextureManager handles caching internally if needed
        return ctx.textureManager.getTextureInfo(texturePath);
    }

    /**
     * Determines the texture resource path for a specific face of a block type.
     * This defines the visual appearance mapping.
     */
    private static String getTexturePathForBlockFace(byte blockType, int faceIndex) {
        switch (blockType) {
            case BlockTypes.BLOCK_DIRT:
                return "/textures/dirt.png";
            case BlockTypes.BLOCK_STONE:
                return "/textures/stone.png";
            case BlockTypes.BLOCK_GRASS:
                // Face indices: 0:Front(+Z), 1:Back(-Z), 2:Top(+Y), 3:Bottom(-Y), 4:Right(+X), 5:Left(-X)
                switch (faceIndex) {
                    case 2:  return "/textures/dirt.png";  // Top face (Placeholder, use correct path)
                    case 3:  return "/textures/dirt.png";       // Bottom face is dirt
                    default: return "/textures/dirt.png"; // Side faces (Placeholder)
                    // Make sure you have these textures: grass_top.png, dirt.png, grass_side.png
                }
            case BlockTypes.BLOCK_WOOD_LOG:
                switch (faceIndex) {
                    case 2: // Top face
                    case 3: // Bottom face
                        return "/textures/wood.png"; // Placeholder
                    default: // Side faces
                        return "/textures/wood.png"; // Placeholder
                    // Make sure you have: wood_log_top.png, wood_log_side.png
                }
            case BlockTypes.BLOCK_GLASS:
                return "/textures/glass.png";
            default:
                // Log once in addFace if fallback needed, no need to log here too.
                // System.err.println("ChunkMesher: Undefined texture path for block type: " + blockType);
                return null; // Return null to indicate missing mapping, fallback handled in addFace
        }
        // NOTE: Replace placeholder paths above (grass_top, grass_side, wood_log_top, wood_log_side)
        // with your actual texture file paths if they differ.
    }

    // --- Array Conversion Helpers ---
    private static float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
}