package com.chaotic_loom.game.rendering.mesh;

import com.chaotic_loom.game.core.Loggers;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class Mesh {
    public static final int POSITION_VBO_ID = 0;
    public static final int TEXTURE_COORDS_VBO_ID = 1;
    public static final int NORMALS_VBO_ID = 2;
    public static final int INSTANCE_MODEL_MATRIX_LOC_START = 3; // mat4 uses 4 locations (3, 4, 5, 6)
    public static final int INSTANCE_UV_OFFSET_LOC = 7;
    public static final int INSTANCE_UV_SCALE_LOC = 8;

    private final int vaoId;
    private final List<Integer> vboIdList; // Store VBO IDs for cleanup
    private final int vertexCount;
    private final int indicesCount; // Number of indices if using EBO

    private int instanceDataVboId; // VBO for combined instance data (matrices, uv info)
    private int maxInstances;      // Current capacity of the instance VBO
    private FloatBuffer instanceDataBuffer; // Reusable buffer for instance data

    public Mesh(float[] positions, float[] textCoords, float[] normals, int[] indices, int initialMaxInstances) {
        FloatBuffer posBuffer = null;
        FloatBuffer textCoordsBuffer = null;
        FloatBuffer vecNormalsBuffer = null;
        IntBuffer indicesBuffer = null;

        try {
            if (positions == null || indices == null) {
                throw new IllegalArgumentException("Positions and indices cannot be null for an indexed mesh.");
            }
            this.vertexCount = positions.length / 3; // 3 components per position
            this.indicesCount = indices.length;
            this.maxInstances = initialMaxInstances > 0 ? initialMaxInstances : 1; // Ensure at least 1
            vboIdList = new ArrayList<>();

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // --- Vertex Data VBOs ---

            // Position VBO (Attribute 0)
            int vboId = glGenBuffers();
            vboIdList.add(vboId);
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(POSITION_VBO_ID, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(POSITION_VBO_ID);

            // Texture Coordinate VBO (Attribute 1)
            if (textCoords != null && textCoords.length > 0) {
                vboId = glGenBuffers();
                vboIdList.add(vboId);
                textCoordsBuffer = MemoryUtil.memAllocFloat(textCoords.length);
                // IMPORTANT: Ensure textCoords length matches vertex count (e.g., textCoords.length == vertexCount * 2)
                if (textCoords.length != this.vertexCount * 2) {
                    Loggers.RENDERER.error("Warning: Texture coordinate array size mismatch for mesh.");
                    // Handle error or provide default coords
                }
                textCoordsBuffer.put(textCoords).flip();
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
                glVertexAttribPointer(TEXTURE_COORDS_VBO_ID, 2, GL_FLOAT, false, 0, 0);
                glEnableVertexAttribArray(TEXTURE_COORDS_VBO_ID);
            } else {
                Loggers.RENDERER.error("Warning: No texture coordinates provided for mesh.");
                // Consider disabling texturing or using default coords if needed
            }


            // Vertex Normal VBO (Attribute 2)
            if (normals != null && normals.length > 0) {
                vboId = glGenBuffers();
                vboIdList.add(vboId);
                vecNormalsBuffer = MemoryUtil.memAllocFloat(normals.length);
                // IMPORTANT: Ensure normals length matches vertex count (e.g., normals.length == vertexCount * 3)
                if (normals.length != this.vertexCount * 3) {
                    Loggers.RENDERER.error("Warning: Normal array size mismatch for mesh.");
                    // Handle error or provide default normals
                }
                vecNormalsBuffer.put(normals).flip();
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, vecNormalsBuffer, GL_STATIC_DRAW);
                glVertexAttribPointer(NORMALS_VBO_ID, 3, GL_FLOAT, false, 0, 0);
                glEnableVertexAttribArray(NORMALS_VBO_ID);
            } else {
                Loggers.RENDERER.error("Warning: No normals provided for mesh.");
                // Consider disabling lighting or using default normals if needed
            }

            // Index VBO (EBO)
            vboId = glGenBuffers();
            vboIdList.add(vboId); // Also track EBO for cleanup
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            // --- Instance Data VBO ---
            instanceDataVboId = glGenBuffers();
            vboIdList.add(instanceDataVboId);

            // Calculate the size needed per instance for the buffer
            // mat4 = 16 floats, vec2 = 2 floats, vec2 = 2 floats => 20 floats per instance
            final int floatsPerInstance = 16 + 2 + 2;
            final int instanceDataSizeBytes = this.maxInstances * floatsPerInstance * Float.BYTES;
            this.instanceDataBuffer = MemoryUtil.memAllocFloat(this.maxInstances * floatsPerInstance); // Allocate CPU buffer

            glBindBuffer(GL_ARRAY_BUFFER, instanceDataVboId);
            glBufferData(GL_ARRAY_BUFFER, instanceDataSizeBytes, GL_DYNAMIC_DRAW); // Allocate GPU buffer

            // --- Configure Instance Attributes ---
            int stride = floatsPerInstance * Float.BYTES; // Stride for the entire instance data block
            long offset = 0;

            // Model Matrix (mat4) - Requires 4 attribute slots (vec4 each)
            for (int i = 0; i < 4; i++) {
                int loc = INSTANCE_MODEL_MATRIX_LOC_START + i;
                glEnableVertexAttribArray(loc);
                glVertexAttribPointer(loc, 4, GL_FLOAT, false, stride, offset);
                glVertexAttribDivisor(loc, 1); // Advance this attribute once per instance
                offset += 4 * Float.BYTES; // Move offset to the next vec4
            }

            // UV Offset (vec2)
            glEnableVertexAttribArray(INSTANCE_UV_OFFSET_LOC);
            glVertexAttribPointer(INSTANCE_UV_OFFSET_LOC, 2, GL_FLOAT, false, stride, offset);
            glVertexAttribDivisor(INSTANCE_UV_OFFSET_LOC, 1); // Advance once per instance
            offset += 2 * Float.BYTES;

            // UV Scale (vec2)
            glEnableVertexAttribArray(INSTANCE_UV_SCALE_LOC);
            glVertexAttribPointer(INSTANCE_UV_SCALE_LOC, 2, GL_FLOAT, false, stride, offset);
            glVertexAttribDivisor(INSTANCE_UV_SCALE_LOC, 1); // Advance once per instance
            // offset += 2 * Float.BYTES; // No need to update offset after the last attribute

            // Unbind buffers to prevent accidental modification
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0); // Unbind VAO **after** configuring EBO and all vertex/instance attributes
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0); // Unbind EBO only after VAO is unbound or setup is complete


        } finally {
            // Free the temporary CPU-side buffers for vertex data
            if (posBuffer != null) MemoryUtil.memFree(posBuffer);
            if (textCoordsBuffer != null) MemoryUtil.memFree(textCoordsBuffer);
            if (vecNormalsBuffer != null) MemoryUtil.memFree(vecNormalsBuffer);
            if (indicesBuffer != null) MemoryUtil.memFree(indicesBuffer);
            // We keep instanceDataBuffer allocated for reuse
        }
    }

    /**
     * Updates the instance data VBO with the provided transformations and UV info.
     * Automatically resizes the VBO if needed.
     *
     * @param transforms List of model matrices for the instances.
     * @param atlasInfo  The TextureAtlasInfo containing UV offset and scale for this batch.
     */
    public void updateInstanceData(List<Matrix4f> transforms, TextureAtlasInfo atlasInfo) {
        int numInstances = transforms.size();
        if (numInstances == 0) return; // Nothing to update

        final int floatsPerInstance = 16 + 2 + 2; // Must match constructor logic

        // --- Resize VBO and CPU buffer if needed ---
        if (numInstances > this.maxInstances) {
            // Calculate new size (e.g., double or 1.5x, or just numInstances)
            this.maxInstances = numInstances; // Or allocate slightly more to avoid frequent resizing
            int newSizeBytes = this.maxInstances * floatsPerInstance * Float.BYTES;

            // Reallocate GPU buffer
            glBindBuffer(GL_ARRAY_BUFFER, instanceDataVboId);
            glBufferData(GL_ARRAY_BUFFER, newSizeBytes, GL_DYNAMIC_DRAW); // Orphan previous buffer & reallocate
            glBindBuffer(GL_ARRAY_BUFFER, 0); // Unbind

            // Reallocate CPU buffer
            MemoryUtil.memFree(this.instanceDataBuffer); // Free old buffer
            this.instanceDataBuffer = MemoryUtil.memAllocFloat(this.maxInstances * floatsPerInstance);

            Loggers.RENDERER.info("Resized instance buffer for mesh VAO {} to {}", vaoId, this.maxInstances);
        }

        // --- Fill CPU buffer ---
        this.instanceDataBuffer.clear();
        Vector2f uvOffset = new Vector2f(atlasInfo.u0(), atlasInfo.v0());
        Vector2f uvScale = new Vector2f(atlasInfo.getWidthUV(), atlasInfo.getHeightUV());

        for (Matrix4f modelMatrix : transforms) {
            // Put matrix (16 floats)
            modelMatrix.get(this.instanceDataBuffer); // Write matrix directly to buffer
            this.instanceDataBuffer.position(this.instanceDataBuffer.position() + 16); // Advance buffer position by 16 floats

            // Put UV offset (2 floats)
            uvOffset.get(this.instanceDataBuffer);
            this.instanceDataBuffer.position(this.instanceDataBuffer.position() + 2);

            // Put UV scale (2 floats)
            uvScale.get(this.instanceDataBuffer);
            this.instanceDataBuffer.position(this.instanceDataBuffer.position() + 2);
        }
        this.instanceDataBuffer.flip(); // Prepare buffer for reading

        // --- Upload data to GPU VBO ---
        glBindBuffer(GL_ARRAY_BUFFER, instanceDataVboId);
        // Upload only the data for the *current* number of instances, not the whole buffer capacity
        glBufferSubData(GL_ARRAY_BUFFER, 0, this.instanceDataBuffer); // Update existing buffer data
        glBindBuffer(GL_ARRAY_BUFFER, 0); // Unbind
    }

    public int getVaoId() {
        return vaoId;
    }

    public int getIndicesCount() {
        return indicesCount;
    }

    public void cleanup() {
        // Disable vertex attribute arrays
        glDisableVertexAttribArray(POSITION_VBO_ID);
        glDisableVertexAttribArray(TEXTURE_COORDS_VBO_ID);
        glDisableVertexAttribArray(NORMALS_VBO_ID);
        for(int i = 0; i < 4; ++i) glDisableVertexAttribArray(INSTANCE_MODEL_MATRIX_LOC_START + i);
        glDisableVertexAttribArray(INSTANCE_UV_OFFSET_LOC);
        glDisableVertexAttribArray(INSTANCE_UV_SCALE_LOC);

        // Delete VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        for (int vboId : vboIdList) {
            glDeleteBuffers(vboId);
        }
        vboIdList.clear();

        // Delete VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);

        if (instanceDataBuffer != null) {
            MemoryUtil.memFree(instanceDataBuffer);
            instanceDataBuffer = null;
        }
    }
}