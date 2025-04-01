package com.chaotic_loom.game.rendering;

import java.util.List;
import java.util.Map;

import com.chaotic_loom.game.rendering.components.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;


public class Renderer {
    private final Logger logger = LogManager.getLogger("Renderer");

    private ShaderProgram defaultShaderProgram;
    private final Vector3f defaultObjectColor = new Vector3f(1.0f, 0.5f, 0.2f);

    /**
     * Initializes the renderer. This includes loading, compiling, and linking shaders,
     * creating uniforms, and enabling OpenGL features like depth testing and backface culling.
     *
     * @param window The window to which the renderer will draw.
     * @throws Exception If there is an error during shader loading, compilation, or linking.
     */
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

    /**
     * Main rendering method. Orchestrates the setup, batch rendering, and cleanup.
     *
     * @param camera           The camera providing projection and view matrices.
     * @param atlasRenderBatch The pre-batched render data, grouped by atlas, mesh, and texture region.
     * @param renderStats      Object to record rendering statistics.
     */
    public void render(
            Window window,
            Camera camera,
            Map<Texture, Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>>> atlasRenderBatch,
            RenderStats renderStats
    ) {
        setupRendering(camera);
        renderAllBatches(atlasRenderBatch, renderStats);
        cleanupRenderingState();
    }

    /**
     * Sets up the initial OpenGL state for rendering the frame.
     * Clears buffers, binds the shader, sets global uniforms, and enables vertex attributes.
     *
     * @param camera The camera used for the frame.
     */
    private void setupRendering(Camera camera) {
        // --- Cleaning ---
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear buffers


        // --- Bind the shader program ONCE before the main loops ---
        defaultShaderProgram.bind();


        // --- Set uniforms that are constant for the entire frame ---
        defaultShaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        defaultShaderProgram.setUniform("viewMatrix", camera.getViewMatrix());
        defaultShaderProgram.setUniform("textureSampler", 0); // Use texture unit 0


        // --- Enable required vertex attributes ONCE (including instance attributes) ---
        // Vertex attributes
        glEnableVertexAttribArray(Mesh.POSITION_VBO_ID);
        glEnableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
        glEnableVertexAttribArray(Mesh.NORMALS_VBO_ID);

        // Instance attributes (Matrix needs 4 slots, UV offset, UV scale)
        for (int i = 0; i < 4; ++i) glEnableVertexAttribArray(Mesh.INSTANCE_MODEL_MATRIX_LOC_START + i);
        glEnableVertexAttribArray(Mesh.INSTANCE_UV_OFFSET_LOC);
        glEnableVertexAttribArray(Mesh.INSTANCE_UV_SCALE_LOC);
    }

    /**
     * Iterates through all pre-batched render data and issues draw calls.
     * Manages texture and mesh state changes to minimize redundant OpenGL calls.
     *
     * @param atlasRenderBatch The pre-batched render data.
     * @param renderStats      Object to record rendering statistics.
     */
    private void renderAllBatches(
            Map<Texture, Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>>> atlasRenderBatch,
            RenderStats renderStats
    ) {
        Texture lastBoundAtlas = null;
        Mesh lastBoundMesh = null;

        // --- Loop 1: Atlas Texture ---
        for (Map.Entry<Texture, Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>>> atlasEntry : atlasRenderBatch.entrySet()) {
            Texture atlasTexture = atlasEntry.getKey();
            Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>> atlasGroup = atlasEntry.getValue();

            renderStats.recordBatchProcessed(); // Counts texture atlas group

            // Bind Atlas Texture (only if changed)
            if (atlasTexture != lastBoundAtlas) {
                glActiveTexture(GL_TEXTURE0); // Ensure texture unit 0 is active
                atlasTexture.bind(0); // Bind the new atlas texture to the textureSampler slot
                lastBoundAtlas = atlasTexture;

                renderStats.recordAtlasBind(); // Counts atlas binding
            }

            // --- Loop 2: Mesh (VAO) ---
            for (Map.Entry<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>> meshEntry : atlasGroup.entrySet()) {
                Mesh sharedMesh = meshEntry.getKey();
                Map<TextureAtlasInfo, List<Matrix4f>> meshGroup = meshEntry.getValue();

                // Bind Mesh VAO (only if changed)
                if (sharedMesh != lastBoundMesh) {
                    glBindVertexArray(sharedMesh.getVaoId());
                    lastBoundMesh = sharedMesh;

                    renderStats.recordMeshBind(); // Counts mesh binding
                }

                // --- Loop 3: Texture Region (AtlasInfo -> Instance Data Upload & Draw Call) ---
                for (Map.Entry<TextureAtlasInfo, List<Matrix4f>> infoEntry : meshGroup.entrySet()) {
                    TextureAtlasInfo atlasInfo = infoEntry.getKey();
                    List<Matrix4f> transforms = infoEntry.getValue();

                    // Perform the actual instanced draw for this specific batch
                    performInstancedDraw(sharedMesh, atlasInfo, transforms, renderStats);
                }
            }
        }
    }

    /**
     * Performs the core instanced drawing operations for a single batch of instances
     * sharing the same mesh and texture atlas region.
     * Updates instance data, sets uniforms, and issues the OpenGL draw call.
     *
     * @param mesh        The mesh to draw.
     * @param atlasInfo   The texture atlas information (UV offset/scale) for this batch.
     * @param transforms  The list of model transformation matrices for the instances.
     * @param renderStats Object to record rendering statistics.
     */
    private void performInstancedDraw(Mesh mesh, TextureAtlasInfo atlasInfo, List<Matrix4f> transforms, RenderStats renderStats) {
        int instanceCount = transforms.size();
        if (instanceCount == 0) {
            return; // Skip if no instances for this specific mesh/texture region
        }

        // 1. Update the Instance Data VBO associated with this mesh
        mesh.updateInstanceData(transforms, atlasInfo);

        // 2. Set any remaining uniforms (e.g., tint color if it varies per batch)
        defaultShaderProgram.setUniform("tintColor", defaultObjectColor); // Example: set tint

        // 3. Issue ONE instanced draw call for all instances in this batch
        glDrawElementsInstanced(GL_TRIANGLES, mesh.getIndicesCount(), GL_UNSIGNED_INT, 0, instanceCount);

        // Record ONE draw call for potentially MANY instances
        renderStats.recordDrawCall();
        renderStats.recordInstancesRendered(instanceCount);
    }

    /**
     * Cleans up OpenGL state after rendering is complete for the frame.
     * Unbinds VAO, texture, shader, and disables vertex attributes.
     */
    private void cleanupRenderingState() {
        // Unbind the last VAO
        glBindVertexArray(0);

        // Unbind texture from unit 0
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);

        // Disable vertex attribute arrays (match enabling)
        glDisableVertexAttribArray(Mesh.POSITION_VBO_ID);
        glDisableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
        glDisableVertexAttribArray(Mesh.NORMALS_VBO_ID);

        // Disable instance attribute arrays
        for (int i = 0; i < 4; ++i) glDisableVertexAttribArray(Mesh.INSTANCE_MODEL_MATRIX_LOC_START + i);
        glDisableVertexAttribArray(Mesh.INSTANCE_UV_OFFSET_LOC);
        glDisableVertexAttribArray(Mesh.INSTANCE_UV_SCALE_LOC);

        // Unbind the shader program
        defaultShaderProgram.unbind();
    }

    /**
     * Cleans the rendering system.
     */
    public void cleanup() {
        if (defaultShaderProgram != null) {
            defaultShaderProgram.cleanup();
        }
    }



    // --- GETTERS ---

    public Logger getLogger() {
        return logger;
    }
}
