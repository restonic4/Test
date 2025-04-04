package com.chaotic_loom.game.rendering.components;

import com.chaotic_loom.game.CubeModelProvider;
import com.chaotic_loom.game.IBlockModelProvider;
import com.chaotic_loom.game.StairsModelProvider;
import com.chaotic_loom.game.registries.built_in.Blocks;
import com.chaotic_loom.game.rendering.TextureManager;
import com.chaotic_loom.game.rendering.mesh.Cube;
import com.chaotic_loom.game.rendering.mesh.Mesh;
import com.chaotic_loom.game.rendering.texture.Texture;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;
import com.chaotic_loom.game.world.ChunkData;
import com.chaotic_loom.game.world.components.Block;
import com.chaotic_loom.game.world.components.BlockInstance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.chaotic_loom.game.core.util.SharedConstants.*;

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
    public static class MeshBuildContext {
        final ChunkData chunkData;
        public final TextureManager textureManager;
        // TODO: Need access to neighbor chunks for seamless meshing across boundaries
        public Texture atlasTexture = null;

        // Geometry Lists
        public final List<Float> positions_opaque = new ArrayList<>();
        public final List<Float> uvs_opaque = new ArrayList<>();
        public final List<Float> normals_opaque = new ArrayList<>();
        public final List<Integer> indices_opaque = new ArrayList<>();
        public int currentVertexOffset_opaque = 0;

        public final List<Float> positions_transparent = new ArrayList<>();
        public final List<Float> uvs_transparent = new ArrayList<>();
        public final List<Float> normals_transparent = new ArrayList<>();
        public final List<Integer> indices_transparent = new ArrayList<>();
        public int currentVertexOffset_transparent = 0;

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
    /**
     * Generates opaque and transparent meshes for the given chunk data.
     */
    public static ChunkMeshBuildResult generateMeshes(ChunkData chunkData, TextureManager textureManager /*, WorldAccessor world */) {

        // CHANGE: Make context accessible (if needed by external providers)
        MeshBuildContext ctx = new MeshBuildContext(chunkData, textureManager /*, world */);

        // Iterate through blocks within the chunk
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_DEPTH; z++) {

                    // CHANGE: Get BlockInstance instead of Block
                    BlockInstance currentInstance = ctx.chunkData.getBlock(x, y, z);
                    Block currentBlock = currentInstance.getBlock();

                    if (currentBlock == Blocks.AIR) continue; // Skip air blocks

                    IBlockModelProvider modelProvider = StairsModelProvider.INSTANCE; // TODO
                    boolean currentIsOpaque = !currentBlock.getSettings().isTransparent();

                    // Define neighbour RELATIVE offsets
                    // Structure: {dx, dy, dz}
                    int[][] neighborOffsets = {
                            { 0,  0,  1}, { 0,  0, -1}, { 0,  1,  0},
                            { 0, -1,  0}, { 1,  0,  0}, {-1,  0,  0}
                    };

                    for (int[] offset : neighborOffsets) {
                        int nx = x + offset[0];
                        int ny = y + offset[1];
                        int nz = z + offset[2];

                        // CHANGE: Get neighbor BlockInstance and Block
                        BlockInstance neighborInstance = ctx.chunkData.getBlock(nx, ny, nz);
                        Block neighborBlock = neighborInstance.getBlock();

                        // Optimization: If neighbor is null (shouldn't happen with new getBlockInstance), treat as AIR.
                        if (neighborBlock == null) neighborBlock = Blocks.AIR;

                        boolean neighborIsOpaque = !neighborBlock.getSettings().isTransparent();


                        // --- Visibility Rule (Occlusion Culling) ---
                        // Render face if:
                        // 1. Current block is opaque AND neighbor is transparent/air
                        // 2. Current block is transparent AND neighbor is air (don't render transparent faces against other transparent/opaque blocks)
                        // 3. Optional: Render if current block and neighbor block are different transparent types (e.g. water against glass) - more complex rule
                        boolean shouldRenderFace = false;
                        if (currentBlock == neighborBlock && currentIsOpaque == neighborIsOpaque) {
                            // Don't render faces between identical blocks (e.g., stone touching stone)
                            shouldRenderFace = false;
                        } else if (currentIsOpaque && !neighborIsOpaque) {
                            shouldRenderFace = true; // Opaque block against transparent/air
                        } else if (!currentIsOpaque && neighborBlock == Blocks.AIR) {
                            shouldRenderFace = true; // Transparent block against air ONLY
                        }
                        // Add more rules if needed (e.g. different transparent blocks)


                        if (shouldRenderFace) {
                            // CHANGE: Use the model provider to add the face

                            // Determine which face needs to be rendered based on the neighbor offset
                            Block.Face faceToRender = modelProvider.getFaceFromNeighborOffset(offset[0], offset[1], offset[2]);

                            if (faceToRender != null) {
                                boolean success = modelProvider.addFaceGeometry(ctx, currentInstance, x, y, z, faceToRender);
                                if (!success) {
                                    System.err.println("ChunkMesher: Failed during addFaceGeometry for " + currentBlock.getIdentifier() + " at " + x + "," + y + "," + z + ". Aborting mesh.");
                                    return null; // Critical error from provider
                                }
                            } else {
                                System.err.println("ChunkMesher: Could not determine face from neighbor offset: " + offset[0]+","+offset[1]+","+offset[2]);
                            }
                        }
                    }
                }
            }
        }

        // Build Mesh Objects from context lists (unchanged)
        Mesh meshOpaque = buildMeshFromContext(ctx.positions_opaque, ctx.uvs_opaque, ctx.normals_opaque, ctx.indices_opaque);
        Mesh meshTransparent = buildMeshFromContext(ctx.positions_transparent, ctx.uvs_transparent, ctx.normals_transparent, ctx.indices_transparent);

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
    private static boolean addFace(MeshBuildContext ctx, boolean isOpaqueFace, int x, int y, int z, Block block, byte faceIndex) {
        // 1. Get Texture Atlas Info
        TextureAtlasInfo atlasInfo = getTextureAtlasInfoForBlock(ctx, block, faceIndex);
        if (atlasInfo == null) {
            // Attempt to use fallback texture
            System.err.printf("ChunkMesher: Missing TextureAtlasInfo for block " + block + ", face %d at [%d,%d,%d]. Using fallback.%n", faceIndex, x, y, z);
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
    private static TextureAtlasInfo getTextureAtlasInfoForBlock(MeshBuildContext ctx, Block block, byte faceIndex) {
        String texturePath = getTexturePathForBlockFace(block, faceIndex);
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
     *
     * Face indices: 0:Front(+Z), 1:Back(-Z), 2:Top(+Y), 3:Bottom(-Y), 4:Right(+X), 5:Left(-X)
     */
    private static String getTexturePathForBlockFace(Block block, byte faceIndex) {
        Map<Block.Face, String> textures = block.getSettings().getFaceProperties().getTextures();
        return textures.get(block.getSettings().getFaceProperties().getFaceFromIndex(faceIndex));
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