package com.chaotic_loom.game.rendering;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    public static final int POSITION_VBO_ID = 0;
    public static final int TEXTURE_COORDS_VBO_ID = 1;
    public static final int NORMALS_VBO_ID = 2;

    private final int vaoId;
    private final List<Integer> vboIdList; // Store VBO IDs for cleanup
    private final int vertexCount;
    private final int indexCount; // Number of indices if using EBO

    // Constructor for indexed meshes (using EBO)
    public Mesh(float[] positions, float[] textCoords, float[] normals, int[] indices) {
        FloatBuffer posBuffer = null;
        FloatBuffer textCoordsBuffer = null;
        FloatBuffer vecNormalsBuffer = null;
        IntBuffer indicesBuffer = null;

        try {
            this.vertexCount = positions.length / 3; // Position has 3 components, x y and z. 1 position = 1 vertex
            this.indexCount = indices.length;
            vboIdList = new ArrayList<>();

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // Position VBO
            int vboId = glGenBuffers();
            vboIdList.add(vboId);
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(POSITION_VBO_ID, 3, GL_FLOAT, false, 0, 0); // Location 0
            glEnableVertexAttribArray(POSITION_VBO_ID);

            // Texture Coordinate VBO - Location 1
            if (textCoords != null && textCoords.length > 0) {
                vboId = glGenBuffers();
                vboIdList.add(vboId);
                textCoordsBuffer = MemoryUtil.memAllocFloat(textCoords.length);
                textCoordsBuffer.put(textCoords).flip();
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
                glVertexAttribPointer(TEXTURE_COORDS_VBO_ID, 2, GL_FLOAT, false, 0, 0); // Location 1
                glEnableVertexAttribArray(TEXTURE_COORDS_VBO_ID);
            }

            // Vertex Normal VBO - Location 2
            if (normals != null && normals.length > 0) {
                vboId = glGenBuffers();
                vboIdList.add(vboId);
                vecNormalsBuffer = MemoryUtil.memAllocFloat(normals.length);
                vecNormalsBuffer.put(normals).flip();
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, vecNormalsBuffer, GL_STATIC_DRAW);
                glVertexAttribPointer(NORMALS_VBO_ID, 3, GL_FLOAT, false, 0, 0); // Location 2
                glEnableVertexAttribArray(NORMALS_VBO_ID);
            }

            // Index VBO (EBO)
            vboId = glGenBuffers();
            vboIdList.add(vboId); // We add the EBO ID for the cleanup method
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            // Unbind VBO and VAO to prevent accidental modification. 0 = unbind
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        } finally {
            // Free the temporary CPU-side buffers
            if (posBuffer != null) MemoryUtil.memFree(posBuffer);
            if (textCoordsBuffer != null) MemoryUtil.memFree(textCoordsBuffer);
            if (vecNormalsBuffer != null) MemoryUtil.memFree(vecNormalsBuffer);
            if (indicesBuffer != null) MemoryUtil.memFree(indicesBuffer);
        }
    }

    public int getVaoId() {
        return vaoId;
    }

    public int getVertexCount() {
        return indexCount > 0 ? indexCount : vertexCount;
    }

    public void cleanup() {
        // Disable vertex attribute arrays
        glDisableVertexAttribArray(POSITION_VBO_ID);
        glDisableVertexAttribArray(TEXTURE_COORDS_VBO_ID);
        glDisableVertexAttribArray(NORMALS_VBO_ID);


        // Delete VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        for (int vboId : vboIdList) {
            glDeleteBuffers(vboId);
        }

        // Delete VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }
}