package com.chaotic_loom.game.rendering;

import com.chaotic_loom.game.rendering.mesh.Cube;
import com.chaotic_loom.game.rendering.mesh.Mesh;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesh {

    public static final int CHUNK_WIDTH = 16;
    public static final int CHUNK_HEIGHT = 16;
    public static final int CHUNK_DEPTH = 16;

    // Simple block data storage. 0 = air, 1 = solid block (for now)
    // Could be expanded to store block IDs for different types/textures
    private byte[][][] blocks;

    // Lists to accumulate mesh data during build
    private List<Float> positions;
    private List<Float> uvs;
    private List<Float> normals;
    private List<Integer> indices;

    public ChunkMesh() {
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
     * @param blockType The type of block (currently 0 for air, 1 for solid)
     */
    public void setBlock(int x, int y, int z, byte blockType) {
        if (isOutOfBounds(x, y, z)) {
            System.err.println("SetBlock coordinates out of bounds: (" + x + "," + y + "," + z + ")");
            return; // Or throw an exception
        }
        blocks[x][y][z] = blockType;
        // Note: In a real game, changing a block would ideally trigger a
        // rebuild of this chunk and potentially neighbor chunks if the
        // change affects face visibility across boundaries.
    }

    /**
     * Gets the block type at the given local chunk coordinates.
     * Returns 0 (air) if coordinates are out of bounds.
     */
    private byte getBlock(int x, int y, int z) {
        if (isOutOfBounds(x, y, z)) {
            return 0; // Treat out-of-bounds as air for face culling
        }
        return blocks[x][y][z];
    }

    /**
     * Checks if the coordinates are outside the chunk's bounds.
     */
    private boolean isOutOfBounds(int x, int y, int z) {
        return x < 0 || x >= CHUNK_WIDTH ||
                y < 0 || y >= CHUNK_HEIGHT ||
                z < 0 || z >= CHUNK_DEPTH;
    }

    /**
     * Generates the Mesh object based on the current block data.
     * Only visible faces between solid blocks and air are included.
     *
     * @return A Mesh object ready for rendering, or null if the chunk is empty.
     */
    public Mesh buildMesh() {
        // Clear previous mesh data
        positions.clear();
        uvs.clear();
        normals.clear();
        indices.clear();

        int currentVertexOffset = 0;

        // Iterate through each block position in the chunk
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_DEPTH; z++) {

                    // If the current block is air, skip it
                    if (blocks[x][y][z] == 0) {
                        continue;
                    }

                    // --- Check adjacent blocks to determine face visibility ---
                    // A face is visible if the adjacent block is air (0)

                    // Front face (+Z) - Check block at Z+1
                    if (getBlock(x, y, z + 1) == 0) {
                        addFace(x, y, z, 0, currentVertexOffset); // Face index 0 = Front
                        currentVertexOffset += 4; // Each face adds 4 vertices
                    }
                    // Back face (-Z) - Check block at Z-1
                    if (getBlock(x, y, z - 1) == 0) {
                        addFace(x, y, z, 1, currentVertexOffset); // Face index 1 = Back
                        currentVertexOffset += 4;
                    }
                    // Top face (+Y) - Check block at Y+1
                    if (getBlock(x, y + 1, z) == 0) {
                        addFace(x, y, z, 2, currentVertexOffset); // Face index 2 = Top
                        currentVertexOffset += 4;
                    }
                    // Bottom face (-Y) - Check block at Y-1
                    if (getBlock(x, y - 1, z) == 0) {
                        addFace(x, y, z, 3, currentVertexOffset); // Face index 3 = Bottom
                        currentVertexOffset += 4;
                    }
                    // Right face (+X) - Check block at X+1
                    if (getBlock(x + 1, y, z) == 0) {
                        addFace(x, y, z, 4, currentVertexOffset); // Face index 4 = Right
                        currentVertexOffset += 4;
                    }
                    // Left face (-X) - Check block at X-1
                    if (getBlock(x - 1, y, z) == 0) {
                        addFace(x, y, z, 5, currentVertexOffset); // Face index 5 = Left
                        currentVertexOffset += 4;
                    }
                }
            }
        }

        // If no vertices were added, return null or an empty mesh
        if (positions.isEmpty()) {
            return null; // Or create a special empty Mesh instance
        }

        // Convert lists to arrays for Mesh constructor
        float[] posArray = toFloatArray(positions);
        float[] uvArray = toFloatArray(uvs);
        float[] normArray = toFloatArray(normals);
        int[] idxArray = toIntArray(indices);

        // Create the actual Mesh object (using your Mesh class)
        // The '100' from the Cube example seemed like a placeholder texture ID.
        // You'll likely manage textures separately.
        // Assuming your Mesh constructor takes these arrays:
        return new Mesh(posArray, uvArray, normArray, idxArray, 100);
        // If your Mesh constructor was exactly as in Cube.createMesh():
        // return new Mesh(posArray, uvArray, normArray, idxArray, 100); // Or relevant texture atlas ID
    }

    /**
     * Adds the vertex, UV, normal, and index data for a specific face
     * of a block at world coordinates (x, y, z).
     *
     * @param x Block's X position within the chunk
     * @param y Block's Y position within the chunk
     * @param z Block's Z position within the chunk
     * @param faceIndex Index of the face to add (0:Front, 1:Back, 2:Top, 3:Bottom, 4:Right, 5:Left)
     * @param baseVertexIndex The starting index for the vertices of this face in the final mesh.
     */
    private void addFace(int x, int y, int z, int faceIndex, int baseVertexIndex) {
        // Calculate starting indices within the Cube's static arrays
        int posStartIndex = faceIndex * 4 * 3; // 4 vertices per face, 3 floats per vertex
        int uvStartIndex = faceIndex * 4 * 2;  // 4 vertices per face, 2 floats per uv
        int normStartIndex = faceIndex * 4 * 3;// 4 vertices per face, 3 floats per normal
        int idxStartIndex = faceIndex * 6;     // 6 indices per face

        // Add the 4 vertices for this face
        for (int i = 0; i < 4; i++) {
            // Position: Get base vertex, offset by block position (x, y, z)
            positions.add(Cube.POSITIONS[posStartIndex + i * 3 + 0] + x);
            positions.add(Cube.POSITIONS[posStartIndex + i * 3 + 1] + y);
            positions.add(Cube.POSITIONS[posStartIndex + i * 3 + 2] + z);

            // UVs: Get base UVs.
            // TODO: Future - Adjust UVs based on blockType for texture atlas
            uvs.add(Cube.BASE_UVS[uvStartIndex + i * 2 + 0]);
            uvs.add(Cube.BASE_UVS[uvStartIndex + i * 2 + 1]);

            // Normals: Get base normals
            normals.add(Cube.NORMALS[normStartIndex + i * 3 + 0]);
            normals.add(Cube.NORMALS[normStartIndex + i * 3 + 1]);
            normals.add(Cube.NORMALS[normStartIndex + i * 3 + 2]);
        }

        // Add the 6 indices for the two triangles of this face
        // Indices need to be offset by the number of vertices already added
        /*for (int i = 0; i < 6; i++) {
            indices.add(baseVertexIndex + Cube.INDICES[idxStartIndex + i]);
        }*/

        indices.add(baseVertexIndex + 0);
        indices.add(baseVertexIndex + 1);
        indices.add(baseVertexIndex + 2);
        indices.add(baseVertexIndex + 0);
        indices.add(baseVertexIndex + 2);
        indices.add(baseVertexIndex + 3);
    }

    // --- Helper methods to convert Lists to primitive arrays ---

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

    // --- Example Usage ---
    public static Mesh test() {
        // 1. Create a chunk
        ChunkMesh chunk = new ChunkMesh();

        // 2. Set some blocks (e.g., a simple flat plane at y=0)
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int z = 0; z < CHUNK_DEPTH; z++) {
                chunk.setBlock(x, 0, z, (byte) 1); // Set solid block
            }
        }
        // Add a pillar
        chunk.setBlock(5, 1, 5, (byte) 1);
        chunk.setBlock(5, 2, 5, (byte) 1);
        chunk.setBlock(5, 3, 5, (byte) 1);

        chunk.setBlock(15, 15, 15, (byte) 1);


        // 3. Build the mesh
        // In a real application, you'd pass this mesh to your rendering system
        // Mesh mesh = chunk.buildMesh();

        // For demonstration, let's print the number of vertices/indices
        // Temporarily store lists before buildMesh clears them
        List<Float> tempPos = new ArrayList<>(chunk.positions);
        List<Integer> tempIdx = new ArrayList<>(chunk.indices);
        Mesh mesh = chunk.buildMesh(); // This clears the internal lists and populates them
        System.out.println("Building mesh for chunk...");
        System.out.println("Total vertices: " + chunk.positions.size() / 3);
        System.out.println("Total floats in position buffer: " + chunk.positions.size());
        System.out.println("Total floats in UV buffer: " + chunk.uvs.size());
        System.out.println("Total floats in normal buffer: " + chunk.normals.size());
        System.out.println("Total indices: " + chunk.indices.size());

        return mesh;
    }
}