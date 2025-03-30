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
            this.vertexCount = positions.length / 3; // Assuming 3 components per position
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
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0); // Location 0
            glEnableVertexAttribArray(0);

            // Texture Coordinate VBO (Optional) - Location 1
            if (textCoords != null && textCoords.length > 0) {
                vboId = glGenBuffers();
                vboIdList.add(vboId);
                textCoordsBuffer = MemoryUtil.memAllocFloat(textCoords.length);
                textCoordsBuffer.put(textCoords).flip();
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
                glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0); // Location 1
                glEnableVertexAttribArray(1);
            }


            // Vertex Normal VBO (Optional) - Location 2
            if (normals != null && normals.length > 0) {
                vboId = glGenBuffers();
                vboIdList.add(vboId);
                vecNormalsBuffer = MemoryUtil.memAllocFloat(normals.length);
                vecNormalsBuffer.put(normals).flip();
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, vecNormalsBuffer, GL_STATIC_DRAW);
                glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0); // Location 2
                glEnableVertexAttribArray(2);
            }

            // Index VBO (EBO)
            vboId = glGenBuffers();
            vboIdList.add(vboId); // Add EBO ID for cleanup too
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            // Unbind VBO and VAO to prevent accidental modification
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            // Do NOT unbind the EBO while the VAO is still bound!
            // glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0); // This is usually done automatically when unbinding VAO

        } finally {
            // Free the temporary CPU-side buffers
            if (posBuffer != null) MemoryUtil.memFree(posBuffer);
            if (textCoordsBuffer != null) MemoryUtil.memFree(textCoordsBuffer);
            if (vecNormalsBuffer != null) MemoryUtil.memFree(vecNormalsBuffer);
            if (indicesBuffer != null) MemoryUtil.memFree(indicesBuffer);
        }
    }

    // Simplified constructor for non-indexed meshes (drawing using glDrawArrays)
    public Mesh(float[] positions) {
        // Similar setup as above, but without indices/EBO
        // Make sure to set indexCount = 0 and vertexCount correctly
        // The draw call in the renderer will need to use glDrawArrays instead of glDrawElements
        // For simplicity, we'll stick to the indexed version for now.
        this(positions, null, null, generateTrivialIndices(positions.length / 3));
        // This ^ is a bit of a hack; ideally, have separate logic or use glDrawArrays.
    }

    private static int[] generateTrivialIndices(int numVertices) {
        int[] indices = new int[numVertices];
        for (int i = 0; i < numVertices; i++) {
            indices[i] = i;
        }
        return indices;
    }


    public int getVaoId() {
        return vaoId;
    }

    public int getVertexCount() {
        // For indexed meshes, the number of indices determines how many vertices are drawn
        return indexCount > 0 ? indexCount : vertexCount;
    }

    /** Use glDrawElements if true, glDrawArrays otherwise. */
    public boolean isIndexed() {
        return this.indexCount > 0;
    }

    public void cleanup() {
        // Disable vertex attribute arrays (optional but good practice)
        // Assuming max 3 attributes for now
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);


        // Delete VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0); // Ensure EBO is unbound before deletion
        for (int vboId : vboIdList) {
            glDeleteBuffers(vboId);
        }

        // Delete VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }
}