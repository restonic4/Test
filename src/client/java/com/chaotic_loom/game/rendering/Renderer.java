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
        defaultShaderProgram.createUniform("tintColor");
        defaultShaderProgram.createUniform("textureSampler");

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
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear buffers

        // Bind the shader program ONCE before the main loops
        defaultShaderProgram.bind();

        // Set uniforms that are constant for the entire frame
        defaultShaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        defaultShaderProgram.setUniform("viewMatrix", camera.getViewMatrix());
        defaultShaderProgram.setUniform("textureSampler", 0); // Use texture unit 0

        Texture lastBoundAtlas = null;
        Mesh lastBoundMesh = null;

        renderStats.recordBatchProcessed(atlasRenderBatch.size()); // Counts texture atlases

        // Enable required vertex attributes ONCE (including instance attributes)
        // Vertex attributes
        glEnableVertexAttribArray(Mesh.POSITION_VBO_ID);
        glEnableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
        glEnableVertexAttribArray(Mesh.NORMALS_VBO_ID);
        // Instance attributes (Matrix needs 4 slots, UV offset, UV scale)
        for(int i = 0; i < 4; ++i) glEnableVertexAttribArray(Mesh.INSTANCE_MODEL_MATRIX_LOC_START + i);
        glEnableVertexAttribArray(Mesh.INSTANCE_UV_OFFSET_LOC);
        glEnableVertexAttribArray(Mesh.INSTANCE_UV_SCALE_LOC);


        // --- Loop 1: Atlas Texture ---
        for (Map.Entry<Texture, Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>>> atlasEntry : atlasRenderBatch.entrySet()) {
            Texture atlasTexture = atlasEntry.getKey();
            Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>> atlasGroup = atlasEntry.getValue();

            // Bind Atlas Texture (only if changed)
            if (atlasTexture != lastBoundAtlas) {
                glActiveTexture(GL_TEXTURE0); // Ensure texture unit 0 is active
                atlasTexture.bind(0);         // Bind the new atlas texture
                lastBoundAtlas = atlasTexture;
                renderStats.recordAtlasBind();
            }

            // --- Loop 2: Mesh (VAO) ---
            for (Map.Entry<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>> meshEntry : atlasGroup.entrySet()) {
                Mesh sharedMesh = meshEntry.getKey();
                Map<TextureAtlasInfo, List<Matrix4f>> meshGroup = meshEntry.getValue();

                // Bind Mesh VAO (only if changed)
                // Includes vertex VBOs, instance VBO, EBO, and attribute pointers/divisors
                if (sharedMesh != lastBoundMesh) {
                    glBindVertexArray(sharedMesh.getVaoId());
                    lastBoundMesh = sharedMesh;
                    renderStats.recordMeshBind();
                }

                // --- Loop 3: Texture Region (AtlasInfo -> Instance Data Upload & Draw Call) ---
                for (Map.Entry<TextureAtlasInfo, List<Matrix4f>> infoEntry : meshGroup.entrySet()) {
                    TextureAtlasInfo atlasInfo = infoEntry.getKey(); // Contains UV offset/scale info
                    List<Matrix4f> transforms = infoEntry.getValue(); // List of model matrices

                    int instanceCount = transforms.size();
                    if (instanceCount == 0) continue; // Skip if no instances for this specific mesh/texture region

                    // *** CORE INSTANCING STEP ***
                    // 1. Update the Instance Data VBO associated with this mesh
                    //    This uploads all model matrices and the *single* UV offset/scale for this batch.
                    sharedMesh.updateInstanceData(transforms, atlasInfo);

                    // 2. Set any remaining uniforms (e.g., tint color if it varies per batch, otherwise set outside loops)
                    defaultShaderProgram.setUniform("tintColor", defaultObjectColor); // Example: set tint

                    // 3. Issue ONE instanced draw call for all instances in this batch
                    //    Uses the currently bound VAO (sharedMesh), EBO (inside VAO), and Shader Program.
                    //    The shader will automatically fetch data from vertex VBOs (per vertex)
                    //    and instance VBOs (per instance) based on the VAO configuration.
                    sharedMesh.renderInstanced(instanceCount);

                    // Record ONE draw call for potentially MANY instances
                    renderStats.recordDrawCall(); // Only one actual GL draw call here!
                    renderStats.recordInstancesRendered(instanceCount); // Track total instances rendered

                    // --- REMOVED: Inner loop drawing individual instances ---
                    // --- REMOVED: Setting modelMatrix, uvOffset, uvScale uniforms per instance ---

                } // End Texture Region (Instancing Batch) Loop
            } // End Mesh Loop
        } // End Atlas Loop

        // --- Cleanup after rendering all batches ---
        glBindVertexArray(0); // Unbind the last VAO

        // Unbind texture from unit 0 (optional but good practice)
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);

        // Disable vertex attribute arrays (match enabling)
        glDisableVertexAttribArray(Mesh.POSITION_VBO_ID);
        glDisableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
        glDisableVertexAttribArray(Mesh.NORMALS_VBO_ID);
        // Disable instance attribute arrays
        for(int i = 0; i < 4; ++i) glDisableVertexAttribArray(Mesh.INSTANCE_MODEL_MATRIX_LOC_START + i);
        glDisableVertexAttribArray(Mesh.INSTANCE_UV_OFFSET_LOC);
        glDisableVertexAttribArray(Mesh.INSTANCE_UV_SCALE_LOC);


        defaultShaderProgram.unbind(); // Unbind the shader program
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