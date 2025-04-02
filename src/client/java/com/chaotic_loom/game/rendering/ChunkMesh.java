package com.chaotic_loom.game.rendering;

import com.chaotic_loom.game.rendering.mesh.Cube;
import com.chaotic_loom.game.rendering.mesh.Mesh;
import com.chaotic_loom.game.rendering.texture.Texture;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChunkMesh {
    public static final int CHUNK_WIDTH = 16;
    public static final int CHUNK_HEIGHT = 16;
    public static final int CHUNK_DEPTH = 16;

    // --- Block Type Definitions ---
    // Define constants for different block types.
    // These IDs will be stored in the 'blocks' array.
    public static final byte BLOCK_AIR = 0;
    public static final byte BLOCK_DIRT = 1;
    public static final byte BLOCK_STONE = 2;
    public static final byte BLOCK_GRASS = 3; // Example: Grass might have different top/side/bottom
    public static final byte BLOCK_WOOD_LOG = 4; // Example: Logs often have different top/side
    public static final byte BLOCK_GLASS = 5;
    // Add more block types as needed...

    // Simple block data storage using block type IDs defined above.
    private byte[][][] blocks;

    // --- Geometry Lists (Duplicated for Opaque and Transparent) ---
    private List<Float> positions_opaque;
    private List<Float> uvs_opaque;
    private List<Float> normals_opaque;
    private List<Integer> indices_opaque;
    private int currentVertexOffset_opaque = 0;

    private List<Float> positions_transparent;
    private List<Float> uvs_transparent;
    private List<Float> normals_transparent;
    private List<Integer> indices_transparent;
    private int currentVertexOffset_transparent = 0;
    // --- End Geometry Lists ---

    // We need the texture manager to look up UV coordinates for block types
    private TextureManager textureManager;
    // Store the single atlas texture used by this chunk mesh. Assumes all blocks are on one atlas.
    private Texture atlasTexture = null;

    /**
     * Holds the result of building the mesh.
     * Includes the mesh itself and the atlas texture it uses.
     */
    public record ChunkMeshBuildResult(
            @Nullable Mesh meshOpaque,
            @Nullable Mesh meshTransparent,
            @Nullable Texture atlasTexture // Single atlas assumed
    ) {}

    public ChunkMesh(TextureManager textureManager) {
        Objects.requireNonNull(textureManager, "TextureManager cannot be null for ChunkMesh");
        this.textureManager = textureManager;
        blocks = new byte[CHUNK_WIDTH][CHUNK_HEIGHT][CHUNK_DEPTH];

        // Initialize both sets of lists
        positions_opaque = new ArrayList<>();
        uvs_opaque = new ArrayList<>();
        normals_opaque = new ArrayList<>();
        indices_opaque = new ArrayList<>();

        positions_transparent = new ArrayList<>();
        uvs_transparent = new ArrayList<>();
        normals_transparent = new ArrayList<>();
        indices_transparent = new ArrayList<>();
    }

    /**
     * Sets the type of block at the given local chunk coordinates.
     *
     * @param x Local X coordinate (0 to CHUNK_WIDTH - 1)
     * @param y Local Y coordinate (0 to CHUNK_HEIGHT - 1)
     * @param z Local Z coordinate (0 to CHUNK_DEPTH - 1)
     * @param blockType The type of block (use constants like BLOCK_DIRT, BLOCK_STONE)
     */
    public void setBlock(int x, int y, int z, byte blockType) {
        if (isOutOfBounds(x, y, z)) {
            System.err.println("SetBlock coordinates out of bounds: (" + x + "," + y + "," + z + ")");
            return; // Or throw an exception
        }
        blocks[x][y][z] = blockType;
        // In a real game, this should mark the chunk as "dirty" and trigger a rebuild.
    }

    /** Gets the block type at the given local chunk coordinates. Returns AIR if out of bounds. */
    private byte getBlock(int x, int y, int z) {
        if (isOutOfBounds(x, y, z)) {
            return BLOCK_AIR; // Treat out-of-bounds as air for face culling
        }
        return blocks[x][y][z];
    }

    /** Checks if the coordinates are outside the chunk's bounds. */
    private boolean isOutOfBounds(int x, int y, int z) {
        return x < 0 || x >= CHUNK_WIDTH ||
                y < 0 || y >= CHUNK_HEIGHT ||
                z < 0 || z >= CHUNK_DEPTH;
    }

    private boolean isBlockOpaque(byte blockType) {
        switch (blockType) {
            case BLOCK_AIR:
            case BLOCK_GLASS: // Glass is not opaque
                return false;
            case BLOCK_DIRT:
            case BLOCK_STONE:
            case BLOCK_GRASS:
            case BLOCK_WOOD_LOG:
            default: // Assume other defined blocks are opaque
                return true;
        }
    }

    /**
     * Checks if a block at the given coordinates is fully surrounded by opaque blocks.
     * Used to optimize away hidden transparent blocks.
     * Assumes the block at (x, y, z) itself is NOT air.
     * @param x block X
     * @param y block Y
     * @param z block Z
     * @return true if all 6 neighbours are opaque, false otherwise.
     */
    private boolean isBlockFullyOccluded(int x, int y, int z) {
        // Check all 6 neighbours
        return isBlockOpaque(getBlock(x, y, z + 1)) &&
                isBlockOpaque(getBlock(x, y, z - 1)) &&
                isBlockOpaque(getBlock(x, y + 1, z)) &&
                isBlockOpaque(getBlock(x, y - 1, z)) &&
                isBlockOpaque(getBlock(x + 1, y, z)) &&
                isBlockOpaque(getBlock(x - 1, y, z));
    }

    /**
     * Generates the Mesh object based on the current block data.
     * Only visible faces between solid blocks and air are included.
     * UV coordinates are calculated based on block type and face index.
     *
     * @return A ChunkMeshBuildResult containing the Mesh and the Texture atlas used,
     * or null if the chunk is empty or an error occurs.
     */
    public ChunkMeshBuildResult buildMesh() {
        // --- Clear previous mesh data for both sets ---
        positions_opaque.clear(); uvs_opaque.clear(); normals_opaque.clear(); indices_opaque.clear();
        positions_transparent.clear(); uvs_transparent.clear(); normals_transparent.clear(); indices_transparent.clear();
        currentVertexOffset_opaque = 0;
        currentVertexOffset_transparent = 0;
        atlasTexture = null;
        // --- End Clear ---


        // Iterate through each block position in the chunk
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_DEPTH; z++) {

                    byte currentBlockType = blocks[x][y][z];

                    // Skip Air blocks
                    if (currentBlockType == BLOCK_AIR) {
                        continue;
                    }

                    boolean currentIsOpaque = isBlockOpaque(currentBlockType);

                    // --- Optimization: Cull fully occluded transparent blocks ---
                    if (!currentIsOpaque) {
                        if (isBlockFullyOccluded(x, y, z)) {
                            continue; // Skip this transparent block entirely
                        }
                    }
                    // --- End Occlusion Check ---


                    // --- Check Neighbours for Face Visibility ---
                    // Define neighbour offsets for cleaner loop
                    int[][] neighbors = {
                            {x, y, z + 1, 0}, // Front (+Z), Face Index 0
                            {x, y, z - 1, 1}, // Back (-Z), Face Index 1
                            {x, y + 1, z, 2}, // Top (+Y), Face Index 2
                            {x, y - 1, z, 3}, // Bottom (-Y), Face Index 3
                            {x + 1, y, z, 4}, // Right (+X), Face Index 4
                            {x - 1, y, z, 5}  // Left (-X), Face Index 5
                    };

                    for (int[] neighborInfo : neighbors) {
                        int nx = neighborInfo[0];
                        int ny = neighborInfo[1];
                        int nz = neighborInfo[2];
                        int faceIndex = neighborInfo[3];

                        byte neighborBlockType = getBlock(nx, ny, nz);
                        boolean neighborIsOpaque = isBlockOpaque(neighborBlockType);

                        // --- Visibility Rule ---
                        // Render face of currentBlock facing neighborBlock IF:
                        // 1. current is Opaque AND neighbor is NOT Opaque
                        // 2. OR current is NOT Opaque AND neighbor is AIR
                        boolean shouldRenderFace = false;
                        if (currentIsOpaque && !neighborIsOpaque) {
                            shouldRenderFace = true;
                        } else if (!currentIsOpaque && neighborBlockType == BLOCK_AIR) {
                            shouldRenderFace = true;
                        }
                        // Add special case? Glass vs Glass face removal already handled by rule 1?
                        // If current is Glass (!Opaque) and neighbor is Glass (!Opaque), rule 1 fails.
                        // If current is Glass (!Opaque) and neighbor is AIR, rule 2 applies (render). OK.
                        // If current is Stone (Opaque) and neighbor is Glass (!Opaque), rule 1 applies (render). OK.

                        if (shouldRenderFace) {
                            // Add face to the correct list (opaque or transparent)
                            boolean success = addFace(currentIsOpaque, x, y, z, currentBlockType, faceIndex);
                            if (!success) {
                                System.err.println("Error adding face for block at " + x + "," + y + "," + z);
                                return null; // Critical error during mesh generation
                            }
                        }
                    } // End neighbour check loop
                } // End z loop
            } // End y loop
        } // End x loop


        // --- Build Mesh Objects ---
        Mesh meshOpaque = null;
        if (!positions_opaque.isEmpty()) {
            float[] pos = toFloatArray(positions_opaque);
            float[] uv = toFloatArray(uvs_opaque);
            float[] norm = toFloatArray(normals_opaque);
            int[] idx = toIntArray(indices_opaque);
            meshOpaque = new Mesh(pos, uv, norm, idx, 100);
        }

        Mesh meshTransparent = null;
        if (!positions_transparent.isEmpty()) {
            float[] pos = toFloatArray(positions_transparent);
            float[] uv = toFloatArray(uvs_transparent);
            float[] norm = toFloatArray(normals_transparent);
            int[] idx = toIntArray(indices_transparent);
            meshTransparent = new Mesh(pos, uv, norm, idx, 100);
        }
        // --- End Build Mesh Objects ---


        // Return null if both are empty (or only if atlasTexture is still null?)
        if (meshOpaque == null && meshTransparent == null) {
            return null; // Entirely empty chunk or only fully occluded blocks
        }


        // Return the result containing both meshes (potentially null) and the atlas
        return new ChunkMeshBuildResult(meshOpaque, meshTransparent, atlasTexture);
    }

    /**
     * Adds face data to the appropriate list (opaque or transparent).
     *
     * @param isOpaqueFace True if the block type is opaque, false if transparent.
     * @param x Block's X position
     * @param y Block's Y position
     * @param z Block's Z position
     * @param blockType The type ID of the block.
     * @param faceIndex Index of the face (0:Front, ..., 5:Left)
     * @return true if successful, false on critical error (e.g., atlas mixing).
     */
    private boolean addFace(boolean isOpaqueFace, int x, int y, int z, byte blockType, int faceIndex) {
        // 1. Get Texture Atlas Info (same as before)
        TextureAtlasInfo atlasInfo = getTextureAtlasInfoForBlock(blockType, faceIndex);
        if (atlasInfo == null) {
            System.err.println("Missing TextureAtlasInfo for block " + blockType + ", face " + faceIndex + ". Using fallback.");
            atlasInfo = textureManager.getTextureInfo("/textures/debug_missing.png");
            if (atlasInfo == null) {
                System.err.println("FATAL: Missing texture fallback /textures/debug_missing.png not found!");
                return false; // Critical if fallback is missing
            }
        }

        // 2. Store/Verify Atlas Texture (same as before)
        if (this.atlasTexture == null) {
            this.atlasTexture = atlasInfo.atlasTexture();
        } else if (this.atlasTexture != atlasInfo.atlasTexture()) {
            System.err.println("CRITICAL ERROR: Chunk mesh generation encountered multiple texture atlases!");
            return false;
        }

        // 3. Calculate start indices in Cube static data (same as before)
        int posStartIndex = faceIndex * 12; // faceIndex * 4 vertices * 3 floats
        int uvStartIndex = faceIndex * 8;   // faceIndex * 4 vertices * 2 floats
        int normStartIndex = faceIndex * 12; // faceIndex * 4 vertices * 3 floats

        // 4. Determine target lists and vertex offset
        List<Float> targetPositions = isOpaqueFace ? positions_opaque : positions_transparent;
        List<Float> targetUvs = isOpaqueFace ? uvs_opaque : uvs_transparent;
        List<Float> targetNormals = isOpaqueFace ? normals_opaque : normals_transparent;
        List<Integer> targetIndices = isOpaqueFace ? indices_opaque : indices_transparent;
        int baseVertexIndex;
        if (isOpaqueFace) {
            baseVertexIndex = currentVertexOffset_opaque;
            currentVertexOffset_opaque += 4; // Increment after use for the next face
        } else {
            baseVertexIndex = currentVertexOffset_transparent;
            currentVertexOffset_transparent += 4; // Increment after use for the next face
        }


        // 5. Add the 4 vertices for this face to the TARGET lists
        for (int i = 0; i < 4; i++) {
            // Position (same calculation)
            targetPositions.add(Cube.POSITIONS[posStartIndex + i * 3 + 0] + x);
            targetPositions.add(Cube.POSITIONS[posStartIndex + i * 3 + 1] + y);
            targetPositions.add(Cube.POSITIONS[posStartIndex + i * 3 + 2] + z);

            // UVs (same calculation)
            float baseU = Cube.BASE_UVS[uvStartIndex + i * 2 + 0];
            float baseV = Cube.BASE_UVS[uvStartIndex + i * 2 + 1];
            float finalU = atlasInfo.u0() + baseU * atlasInfo.getWidthUV();
            float finalV = atlasInfo.v0() + baseV * atlasInfo.getHeightUV();
            targetUvs.add(finalU);
            targetUvs.add(finalV);

            // Normals (same calculation)
            targetNormals.add(Cube.NORMALS[normStartIndex + i * 3 + 0]);
            targetNormals.add(Cube.NORMALS[normStartIndex + i * 3 + 1]);
            targetNormals.add(Cube.NORMALS[normStartIndex + i * 3 + 2]);
        }

        // 6. Add the 6 indices to the TARGET list
        targetIndices.add(baseVertexIndex + 0);
        targetIndices.add(baseVertexIndex + 1);
        targetIndices.add(baseVertexIndex + 2);
        targetIndices.add(baseVertexIndex + 0);
        targetIndices.add(baseVertexIndex + 2);
        targetIndices.add(baseVertexIndex + 3);

        return true; // Success
    }

    /**
     * Determines which texture resource path to use for a given block type and face index.
     * This is where you define the appearance of your blocks.
     *
     * @param blockType The ID of the block.
     * @param faceIndex The index of the face (0:Front(+Z), 1:Back(-Z), 2:Top(+Y), 3:Bottom(-Y), 4:Right(+X), 5:Left(-X)).
     * @return The resource path (e.g., "/textures/blocks/dirt.png") or null if undefined.
     */
    private String getTexturePathForBlockFace(byte blockType, int faceIndex) {
        // This mapping could be loaded from a configuration file (JSON, etc.) for more flexibility.
        switch (blockType) {
            case BLOCK_DIRT:
                return "/textures/dirt.png"; // Dirt is the same on all sides
            case BLOCK_STONE:
                return "/textures/stone.png"; // Stone is the same on all sides
            case BLOCK_GRASS:
                switch (faceIndex) {
                    case 2: return "/textures/dirt.png"; // Top face
                    case 3: return "/textures/dirt.png";      // Bottom face
                    default: return "/textures/dirt.png"; // Side faces (Front, Back, Left, Right)
                }
            case BLOCK_WOOD_LOG:
                switch (faceIndex) {
                    case 2: // Top face
                    case 3: // Bottom face
                        return "/textures/wood.png";
                    default: // Side faces
                        return "/textures/wood.png";
                }
            case BLOCK_GLASS:
                return "/textures/glass.png";
            default:
                System.err.println("Undefined texture mapping for block type: " + blockType);
                return null; // Or return a default/missing texture path
        }
    }

    /**
     * Helper method to get TextureAtlasInfo using the TextureManager.
     *
     * @param blockType The ID of the block.
     * @param faceIndex The index of the face.
     * @return TextureAtlasInfo or null if the texture path is not found or not mapped.
     */
    private TextureAtlasInfo getTextureAtlasInfoForBlock(byte blockType, int faceIndex) {
        String texturePath = getTexturePathForBlockFace(blockType, faceIndex);
        if (texturePath == null) {
            return null; // No texture defined for this block/face combination
        }
        return textureManager.getTextureInfo(texturePath);
    }


    // --- Helper methods to convert Lists to primitive arrays ---
    // (Keep these as they are efficient for this purpose)
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    // --- Example Usage (Updated) ---
    // Note: This test method now requires a valid TextureManager instance.
    public static ChunkMeshBuildResult test(TextureManager textureManager) {
        System.out.println("Building test chunk mesh (Opaque + Transparent)...");
        ChunkMesh chunk = new ChunkMesh(textureManager);

        // --- Set Blocks ---
        // Floor with some glass patches
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int z = 0; z < CHUNK_DEPTH; z++) {
                chunk.setBlock(x, 0, z, BLOCK_STONE); // Base layer
                if (x > 4 && x < 8 && z > 4 && z < 8) {
                    chunk.setBlock(x, 1, z, BLOCK_GLASS); // Glass panel
                } else if (x % 4 == 0 && z % 4 == 0) {
                    chunk.setBlock(x, 1, z, BLOCK_GRASS);
                } else {
                    chunk.setBlock(x, 1, z, BLOCK_DIRT);
                }
            }
        }
        // Pillar with glass section
        chunk.setBlock(5, 2, 5, BLOCK_WOOD_LOG);
        chunk.setBlock(5, 3, 5, BLOCK_GLASS); // Glass in the middle
        chunk.setBlock(5, 4, 5, BLOCK_WOOD_LOG);

        // Floating glass block
        chunk.setBlock(15, 15, 15, BLOCK_GLASS);

        // Fully occluded glass block (should be ignored by optimization)
        chunk.setBlock(1, 1, 1, BLOCK_GLASS);
        chunk.setBlock(1, 1, 2, BLOCK_STONE);
        chunk.setBlock(1, 1, 0, BLOCK_STONE);
        chunk.setBlock(1, 2, 1, BLOCK_STONE);
        chunk.setBlock(1, 0, 1, BLOCK_STONE); // Already stone from floor
        chunk.setBlock(2, 1, 1, BLOCK_STONE);
        chunk.setBlock(0, 1, 1, BLOCK_STONE);
        // --- End Set Blocks ---


        // Build Meshes
        ChunkMeshBuildResult result = chunk.buildMesh();

        // --- Print Stats ---
        if (result != null && result.atlasTexture() != null) {
            System.out.println("Test chunk build completed.");
            System.out.println("Using Atlas Texture ID: " + result.atlasTexture().getTextureId());

            if (result.meshOpaque() != null) {
                System.out.println(" Opaque Mesh VAO ID: " + result.meshOpaque().getVaoId() +
                        //", Vertices: " + result.meshOpaque().getVertexCount() +
                        ", Indices: " + result.meshOpaque().getIndicesCount());
            } else { System.out.println(" Opaque Mesh: None"); }

            if (result.meshTransparent() != null) {
                System.out.println(" Transparent Mesh VAO ID: " + result.meshTransparent().getVaoId() +
                        //", Vertices: " + result.meshTransparent().getVertexCount() +
                        ", Indices: " + result.meshTransparent().getIndicesCount());
            } else { System.out.println(" Transparent Mesh: None"); }

        } else { System.err.println("Test chunk mesh build failed or resulted in empty geometry!"); }
        System.out.println("-----");
        // --- End Print Stats ---

        return result;
    }
}