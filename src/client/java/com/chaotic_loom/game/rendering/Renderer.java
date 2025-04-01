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

    public void render(
            Window window,
            Camera camera,
            Map<Texture, Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>>> atlasRenderBatch,
            RenderStats renderStats
    ) {
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

        renderStats.recordBatchProcessed(atlasRenderBatch.size());

        // --- Loop 1: Atlas Texture ---
        for (Map.Entry<Texture, Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>>> atlasEntry : atlasRenderBatch.entrySet()) {
            Texture atlasTexture = atlasEntry.getKey();
            Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>> atlasGroup = atlasEntry.getValue();

            // Bind Atlas Texture (only if changed)
            if (atlasTexture != lastBoundAtlas) {
                atlasTexture.bind(0);
                lastBoundAtlas = atlasTexture;
                renderStats.recordAtlasBind();
            }

            // --- Loop 2: Mesh (VAO) ---
            for (Map.Entry<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>> meshEntry : atlasGroup.entrySet()) {
                Mesh sharedMesh = meshEntry.getKey();
                Map<TextureAtlasInfo, List<Matrix4f>> meshGroup = meshEntry.getValue();

                // Bind Mesh VAO (only if changed)
                if (sharedMesh != lastBoundMesh) {
                    glBindVertexArray(sharedMesh.getVaoId());
                    lastBoundMesh = sharedMesh;
                    renderStats.recordMeshBind();
                }

                // --- Loop 3: Texture Region (AtlasInfo -> UV Uniforms) ---
                for (Map.Entry<TextureAtlasInfo, List<Matrix4f>> infoEntry : meshGroup.entrySet()) {
                    TextureAtlasInfo atlasInfo = infoEntry.getKey();
                    List<Matrix4f> transforms = infoEntry.getValue();

                    if (transforms.isEmpty()) continue;

                    // Set UV Uniforms ONCE for this group of instances
                    uvOffsetVec.set(atlasInfo.u0, atlasInfo.v0);
                    uvScaleVec.set(atlasInfo.getWidthUV(), atlasInfo.getHeightUV());
                    defaultShaderProgram.setUniform("uvOffset", uvOffsetVec);
                    defaultShaderProgram.setUniform("uvScale", uvScaleVec);

                    // --- Loop 4: Instances (Transforms) ---
                    // Draw all instances using the currently bound Atlas, Mesh, and UV params
                    for (Matrix4f modelMatrix : transforms) {
                        defaultShaderProgram.setUniform("modelMatrix", modelMatrix);
                        defaultShaderProgram.setUniform("tintColor", defaultObjectColor); // Still default tint

                        // Actual Draw Call
                        glDrawElements(GL_TRIANGLES, sharedMesh.getVertexCount(), GL_UNSIGNED_INT, 0);
                        renderStats.recordDrawCall(1);
                    }
                    // --- End Instance Loop ---

                } // End Texture Region Loop
            } // End Mesh Loop
        } // End Atlas Loop

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