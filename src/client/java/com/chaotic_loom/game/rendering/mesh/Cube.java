package com.chaotic_loom.game.rendering.mesh;

public class Cube {

    // Cube vertices (Pos: x,y,z)
    public static final float[] POSITIONS = {
            // Front face
            -0.5f, -0.5f,  0.5f,
            0.5f, -0.5f,  0.5f,
            0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            // Back face
            -0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
            0.5f,  0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
            // Top face
            -0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f,  0.5f,
            0.5f,  0.5f,  0.5f,
            0.5f,  0.5f, -0.5f,
            // Bottom face
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
            0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
            // Right face
            0.5f, -0.5f, -0.5f,
            0.5f,  0.5f, -0.5f,
            0.5f,  0.5f,  0.5f,
            0.5f, -0.5f,  0.5f,
            // Left face
            -0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f, -0.5f,
    };

    // Indices for vertices order (defining triangles)
    public static final int[] INDICES = {
            // Front face
            0, 1, 2, 0, 2, 3,
            // Back face
            4, 5, 6, 4, 6, 7,
            // Top face
            8, 9, 10, 8, 10, 11,
            // Bottom face
            12, 13, 14, 12, 14, 15,
            // Right face
            16, 17, 18, 16, 18, 19,
            // Left face
            20, 21, 22, 20, 22, 23
    };

    public static final float[] NORMALS = {
            // Front (+Z)
            0, 0, 1,   0, 0, 1,   0, 0, 1,   0, 0, 1,
            // Back (-Z)
            0, 0,-1,   0, 0,-1,   0, 0,-1,   0, 0,-1,
            // Top (+Y)
            0, 1, 0,   0, 1, 0,   0, 1, 0,   0, 1, 0,
            // Bottom (-Y)
            0,-1, 0,   0,-1, 0,   0,-1, 0,   0,-1, 0,
            // Right (+X)
            1, 0, 0,   1, 0, 0,   1, 0, 0,   1, 0, 0,
            // Left (-X)
            -1, 0, 0,  -1, 0, 0,  -1, 0, 0,  -1, 0, 0
    };

    public static final float[] BASE_UVS = {
            // Front Face
            0, 0,   1, 0,   1, 1,   0, 1,
            // Back Face
            0, 0,   1, 0,   1, 1,   0, 1, // May need flipping depending on modeler/texture
            // Top Face
            0, 0,   1, 0,   1, 1,   0, 1,
            // Bottom Face
            0, 0,   1, 0,   1, 1,   0, 1,
            // Right Face
            0, 0,   1, 0,   1, 1,   0, 1,
            // Left Face
            0, 0,   1, 0,   1, 1,   0, 1,
    };

    public static Mesh createMesh() {
        // Currently only positions and indices
        return new Mesh(POSITIONS, BASE_UVS, NORMALS, INDICES, 100);
    }
}