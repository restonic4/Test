package com.chaotic_loom.game.rendering;

import java.util.List;
import java.util.Map;

import com.chaotic_loom.game.events.WindowEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.chaotic_loom.game.components.ClientGameObject;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;


public class Renderer {
    private final Logger logger = LogManager.getLogger("Renderer");

    private ShaderProgram defaultShaderProgram;
    private final Vector3f defaultObjectColor = new Vector3f(1.0f, 0.5f, 0.2f);

    // Cache
    private final Vector2f uvOffsetVec = new Vector2f();
    private final Vector2f uvScaleVec = new Vector2f();

    public void init(Window window) throws Exception {
        // Load, compile, link shaders
        defaultShaderProgram = new ShaderProgram();
        defaultShaderProgram.createVertexShader(ShaderProgram.loadShaderResource("/shaders/default.vert"));
        defaultShaderProgram.createFragmentShader(ShaderProgram.loadShaderResource("/shaders/default.frag"));
        defaultShaderProgram.link();

        // Create uniforms
        defaultShaderProgram.createUniform("projectionMatrix");
        defaultShaderProgram.createUniform("viewMatrix");
        defaultShaderProgram.createUniform("modelMatrix");
        defaultShaderProgram.createUniform("tintColor");
        defaultShaderProgram.createUniform("textureSampler");

        // Atlas UVs
        defaultShaderProgram.createUniform("uvOffset");
        defaultShaderProgram.createUniform("uvScale");

        // Enable Depth Testing (Important for 3D)
        glEnable(GL_DEPTH_TEST);

        // Enable backface culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    public void render(Window window, Camera camera, Map<RenderBatchKey, List<InstanceData>> renderBatch, RenderStats renderStats) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Enable common attributes once
        glEnableVertexAttribArray(Mesh.POSITION_VBO_ID);
        glEnableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
        glEnableVertexAttribArray(Mesh.NORMALS_VBO_ID);

        defaultShaderProgram.bind();

        // Upload camera matrices
        defaultShaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        defaultShaderProgram.setUniform("viewMatrix", camera.getViewMatrix());

        Texture lastBoundAtlas = null;
        Mesh lastBoundMesh = null;

        renderStats.recordBatchProcessed(renderBatch.size());

        // Iterate through each batch (grouped by Atlas and Mesh via the Key)
        for (Map.Entry<RenderBatchKey, List<InstanceData>> entry : renderBatch.entrySet()) {
            RenderBatchKey key = entry.getKey();
            List<InstanceData> instances = entry.getValue();

            if (instances.isEmpty()) continue; // Skip empty batches

            Texture currentAtlas = key.atlasTexture();
            Mesh currentMesh = key.mesh();

            // --- Bind Atlas if changed ---
            if (currentAtlas != lastBoundAtlas) {
                currentAtlas.bind(0); // Bind to texture unit 0
                lastBoundAtlas = currentAtlas;

                renderStats.recordAtlasBind();
            }

            // --- Bind Mesh VAO if changed ---
            if (currentMesh != lastBoundMesh) {
                // Optional optimization: If VAO ID is same, skip rebind?
                // if (lastBoundMesh == null || currentMesh.getVaoId() != lastBoundMesh.getVaoId()) {
                glBindVertexArray(currentMesh.getVaoId());
                lastBoundMesh = currentMesh;

                renderStats.recordMeshBind();
                // }
            }

            // --- Iterate through instances in this batch ---
            for (InstanceData instance : instances) {
                TextureAtlasInfo atlasInfo = instance.atlasInfo();
                Matrix4f modelMatrix = instance.modelMatrix();

                // Calculate and Set UV Uniforms for this instance
                // NOTE: This still sets uniforms per instance! See optimization note below.
                uvOffsetVec.set(atlasInfo.u0, atlasInfo.v0);
                uvScaleVec.set(atlasInfo.getWidthUV(), atlasInfo.getHeightUV());
                defaultShaderProgram.setUniform("uvOffset", uvOffsetVec);
                defaultShaderProgram.setUniform("uvScale", uvScaleVec);

                // Set other per-instance uniforms
                defaultShaderProgram.setUniform("modelMatrix", modelMatrix);
                defaultShaderProgram.setUniform("tintColor", defaultObjectColor); // Still using default tint

                // Draw the instance
                glDrawElements(GL_TRIANGLES, currentMesh.getVertexCount(), GL_UNSIGNED_INT, 0);

                renderStats.recordDrawCall(1);
            }
        } // End loop through batches

        // Unbind resources after all batches are drawn
        glBindVertexArray(0); // Unbind VAO
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0); // Unbind texture from unit 0

        // Disable attributes
        glDisableVertexAttribArray(Mesh.POSITION_VBO_ID);
        glDisableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
        glDisableVertexAttribArray(Mesh.NORMALS_VBO_ID);

        defaultShaderProgram.unbind();
    }

    public void cleanup() {
        if (defaultShaderProgram != null) {
            defaultShaderProgram.cleanup();
        }
    }

    public Logger getLogger() {
        return logger;
    }
}