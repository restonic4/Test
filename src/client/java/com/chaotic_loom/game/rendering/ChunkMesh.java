package com.chaotic_loom.game.rendering;

import com.chaotic_loom.game.rendering.mesh.Cube;
import com.chaotic_loom.game.rendering.mesh.Mesh;
import com.chaotic_loom.game.rendering.texture.Texture;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;

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
    // Add more block types as needed...

    // Simple block data storage using block type IDs defined above.
    private byte[][][] blocks;

    // Lists to accumulate mesh data during build
    private List<Float> positions;
    private List<Float> uvs;
    private List<Float> normals;
    private List<Integer> indices;

    // We need the texture manager to look up UV coordinates for block types
    private TextureManager textureManager;
    // Store the single atlas texture used by this chunk mesh. Assumes all blocks are on one atlas.
    private Texture atlasTexture = null;

    /**
     * Holds the result of building the mesh.
     * Includes the mesh itself and the atlas texture it uses.
     */
    public record ChunkMeshBuildResult(Mesh mesh, Texture atlasTexture) {}

    public ChunkMesh(TextureManager textureManager) {
        // Require TextureManager for UV lookups
        Objects.requireNonNull(textureManager, "TextureManager cannot be null for ChunkMesh");
        this.textureManager = textureManager;
        blocks = new byte[CHUNK_WIDTH][CHUNK_HEIGHT][CHUNK_DEPTH]; // Defaults to 0 (air)
        positions = new ArrayList<>();
        uvs = new ArrayList<>();
        normals = new ArrayList<>();
        indices = new ArrayList<>();
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

    /**
     * Generates the Mesh object based on the current block data.
     * Only visible faces between solid blocks and air are included.
     * UV coordinates are calculated based on block type and face index.
     *
     * @return A ChunkMeshBuildResult containing the Mesh and the Texture atlas used,
     * or null if the chunk is empty or an error occurs.
     */
    public ChunkMeshBuildResult buildMesh() {
        // Clear previous mesh data
        positions.clear();
        uvs.clear();
        normals.clear();
        indices.clear();
        atlasTexture = null; // Reset the atlas texture reference for this build

        int currentVertexOffset = 0;

        // Iterate through each block position in the chunk
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_DEPTH; z++) {

                    byte currentBlockType = blocks[x][y][z];

                    // If the current block is air, skip it
                    if (currentBlockType == BLOCK_AIR) {
                        continue;
                    }

                    // --- Check adjacent blocks to determine face visibility ---
                    // A face is visible if the adjacent block is air (BLOCK_AIR)

                    // Front face (+Z) - Check block at Z+1
                    if (getBlock(x, y, z + 1) == BLOCK_AIR) {
                        if (addFace(x, y, z, currentBlockType, 0, currentVertexOffset)) { // Face index 0 = Front
                            currentVertexOffset += 4; // Each face adds 4 vertices
                        } else return null; // Error adding face
                    }
                    // Back face (-Z) - Check block at Z-1
                    if (getBlock(x, y, z - 1) == BLOCK_AIR) {
                        if (addFace(x, y, z, currentBlockType, 1, currentVertexOffset)) { // Face index 1 = Back
                            currentVertexOffset += 4;
                        } else return null;
                    }
                    // Top face (+Y) - Check block at Y+1
                    if (getBlock(x, y + 1, z) == BLOCK_AIR) {
                        if (addFace(x, y, z, currentBlockType, 2, currentVertexOffset)) { // Face index 2 = Top
                            currentVertexOffset += 4;
                        } else return null;
                    }
                    // Bottom face (-Y) - Check block at Y-1
                    if (getBlock(x, y - 1, z) == BLOCK_AIR) {
                        if (addFace(x, y, z, currentBlockType, 3, currentVertexOffset)) { // Face index 3 = Bottom
                            currentVertexOffset += 4;
                        } else return null;
                    }
                    // Right face (+X) - Check block at X+1
                    if (getBlock(x + 1, y, z) == BLOCK_AIR) {
                        if (addFace(x, y, z, currentBlockType, 4, currentVertexOffset)) { // Face index 4 = Right
                            currentVertexOffset += 4;
                        } else return null;
                    }
                    // Left face (-X) - Check block at X-1
                    if (getBlock(x - 1, y, z) == BLOCK_AIR) {
                        if (addFace(x, y, z, currentBlockType, 5, currentVertexOffset)) { // Face index 5 = Left
                            currentVertexOffset += 4;
                        } else return null;
                    }
                }
            }
        }

        // If no vertices were added, return null
        if (positions.isEmpty() || atlasTexture == null) {
            return null; // Or create a special empty Mesh instance/result
        }

        // Convert lists to arrays for Mesh constructor
        float[] posArray = toFloatArray(positions);
        float[] uvArray = toFloatArray(uvs);
        float[] normArray = toFloatArray(normals);
        int[] idxArray = toIntArray(indices);

        // Create the actual Mesh object
        // The texture ID argument in the Mesh constructor might not be needed
        // if the renderer gets the texture from the TextureAtlasInfo / RenderBatch.
        // Assuming Mesh constructor ONLY needs vertex data now.
        // Adjust the Mesh constructor if it still expects a texture ID.
        Mesh mesh = new Mesh(posArray, uvArray, normArray, idxArray, 100);

        // Return the Mesh and the Atlas Texture it uses
        return new ChunkMeshBuildResult(mesh, atlasTexture);
    }

    /**
     * Adds the vertex, UV, normal, and index data for a specific face
     * of a block at world coordinates (x, y, z). Calculates UVs based on block type.
     *
     * @param x Block's X position within the chunk
     * @param y Block's Y position within the chunk
     * @param z Block's Z position within the chunk
     * @param blockType The type ID of the block being added.
     * @param faceIndex Index of the face (0:Front, 1:Back, 2:Top, 3:Bottom, 4:Right, 5:Left)
     * @param baseVertexIndex The starting index for the vertices of this face.
     * @return true if the face was added successfully, false if texture info was missing.
     */
    private boolean addFace(int x, int y, int z, byte blockType, int faceIndex, int baseVertexIndex) {
        // 1. Get Texture Atlas Info for this specific block type and face
        TextureAtlasInfo atlasInfo = getTextureAtlasInfoForBlock(blockType, faceIndex);

        if (atlasInfo == null) {
            System.err.println("Missing TextureAtlasInfo for block type " + blockType + ", face " + faceIndex + ". Skipping face.");
            // Optionally, use a default "missing texture" instead of skipping
            atlasInfo = textureManager.getTextureInfo("/textures/debug_missing.png"); // Example fallback
            if (atlasInfo == null) {
                System.err.println("FATAL: Missing texture fallback /textures/debug_missing.png not found!");
                return false; // Critical error if even the fallback is missing
            }
        }

        // 2. Store/Verify the Atlas Texture (ensure all faces in a chunk use the same atlas)
        if (this.atlasTexture == null) {
            this.atlasTexture = atlasInfo.atlasTexture();
        } else if (this.atlasTexture != atlasInfo.atlasTexture()) {
            // This should ideally not happen if TextureManager puts all block textures on one atlas.
            // If you support multiple atlases for chunks, this logic needs expansion.
            System.err.println("CRITICAL ERROR: Chunk mesh generation encountered multiple texture atlases! Block: " + blockType);
            return false; // Cannot build mesh with faces from different atlases easily
        }

        // 3. Calculate start indices within Cube's static data
        int posStartIndex = faceIndex * 4 * 3; // 4 vertices per face, 3 floats per vertex pos
        int uvStartIndex = faceIndex * 4 * 2;  // 4 vertices per face, 2 floats per vertex uv
        int normStartIndex = faceIndex * 4 * 3;// 4 vertices per face, 3 floats per vertex normal

        // 4. Add the 4 vertices for this face
        for (int i = 0; i < 4; i++) {
            // --- Position ---
            // Get base vertex position relative to (0,0,0) and offset by block's position (x, y, z)
            positions.add(Cube.POSITIONS[posStartIndex + i * 3 + 0] + x);
            positions.add(Cube.POSITIONS[posStartIndex + i * 3 + 1] + y);
            positions.add(Cube.POSITIONS[posStartIndex + i * 3 + 2] + z);

            // --- UVs (Texture Coordinates) ---
            // Get the base UV for this vertex of the face (ranges 0.0 to 1.0)
            float baseU = Cube.BASE_UVS[uvStartIndex + i * 2 + 0];
            float baseV = Cube.BASE_UVS[uvStartIndex + i * 2 + 1];

            // Calculate the final UV coordinate within the atlas using the TextureAtlasInfo
            // Formula: finalUV = atlasStartUV + baseUV * atlasUVSize
            float finalU = atlasInfo.u0() + baseU * atlasInfo.getWidthUV();
            float finalV = atlasInfo.v0() + baseV * atlasInfo.getHeightUV();

            uvs.add(finalU);
            uvs.add(finalV);

            // --- Normals ---
            // Get base normals (they are the same regardless of texture)
            normals.add(Cube.NORMALS[normStartIndex + i * 3 + 0]);
            normals.add(Cube.NORMALS[normStartIndex + i * 3 + 1]);
            normals.add(Cube.NORMALS[normStartIndex + i * 3 + 2]);
        }

        // 5. Add the 6 indices for the two triangles forming this face quad
        // Indices are relative to the start of *this chunk's mesh*, offset by baseVertexIndex.
        indices.add(baseVertexIndex + 0);
        indices.add(baseVertexIndex + 1);
        indices.add(baseVertexIndex + 2);
        indices.add(baseVertexIndex + 0);
        indices.add(baseVertexIndex + 2);
        indices.add(baseVertexIndex + 3);

        return true; // Face added successfully
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
                // Add cases for other block types...
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
        System.out.println("Building test chunk mesh...");

        // 1. Create a chunk (now requires TextureManager)
        ChunkMesh chunk = new ChunkMesh(textureManager);

        // 2. Set some blocks with different types
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int z = 0; z < CHUNK_DEPTH; z++) {
                chunk.setBlock(x, 0, z, BLOCK_STONE); // Floor of stone
                if (x % 4 == 0 && z % 4 == 0) {
                    chunk.setBlock(x, 1, z, BLOCK_GRASS); // Place some grass blocks
                } else {
                    chunk.setBlock(x, 1, z, BLOCK_DIRT); // Place some dirt blocks
                }
            }
        }
        // Add a pillar of logs
        chunk.setBlock(5, 1, 5, BLOCK_WOOD_LOG);
        chunk.setBlock(5, 2, 5, BLOCK_WOOD_LOG);
        chunk.setBlock(5, 3, 5, BLOCK_WOOD_LOG);

        // Single floating block
        chunk.setBlock(15, 15, 15, BLOCK_STONE);


        // 3. Build the mesh
        ChunkMeshBuildResult result = chunk.buildMesh();

        // For demonstration, print stats if successful
        if (result != null && result.mesh() != null) {
            System.out.println("Test chunk mesh built successfully.");
            // Accessing internal lists after buildMesh is unreliable as they are reused.
            // Get stats from the final mesh object.
            System.out.println("Final Mesh VAO ID: " + result.mesh().getVaoId());
            //System.out.println("Total vertices: " + result.mesh().getVertexCount());
            System.out.println("Total indices: " + result.mesh().getIndicesCount());
            System.out.println("Using Atlas Texture ID: " + result.atlasTexture().getTextureId());
            System.out.println("-----");
        } else {
            System.err.println("Test chunk mesh build failed!");
            System.out.println("-----");
        }


        return result; // Return the result (could be null)
    }
}