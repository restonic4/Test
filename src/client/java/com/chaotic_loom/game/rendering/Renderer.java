package com.chaotic_loom.game.rendering;

import java.util.List;
import java.util.Map;

import com.chaotic_loom.game.events.WindowEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.chaotic_loom.game.components.ClientGameObject;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;


public class Renderer {
    private final Logger logger = LogManager.getLogger("Renderer");

    private ShaderProgram defaultShaderProgram;
    private final Vector3f defaultObjectColor = new Vector3f(1.0f, 0.5f, 0.2f);

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

    public void render(Window window, Camera camera, Map<Texture, Map<Mesh, List<Matrix4f>>> atlasRenderBatch) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        defaultShaderProgram.bind();

        // Upload camera matrices
        defaultShaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        defaultShaderProgram.setUniform("viewMatrix", camera.getViewMatrix());

        // Iterate through each Atlas Texture used in the batch
        for (Map.Entry<Texture, Map<Mesh, List<Matrix4f>>> atlasEntry : atlasRenderBatch.entrySet()) {
            Texture atlasTexture = atlasEntry.getKey();
            Map<Mesh, List<Matrix4f>> meshBatch = atlasEntry.getValue();

            // --- Bind the specific Atlas Texture for this group ---
            atlasTexture.bind(0); // Bind to texture unit 0

            boolean vaoBound = false;

            // --- Iterate through Meshes using this Atlas ---
            for (Map.Entry<Mesh, List<Matrix4f>> meshEntry : meshBatch.entrySet()) {
                Mesh mesh = meshEntry.getKey();
                List<Matrix4f> transforms = meshEntry.getValue();

                // Bind the VAO for this mesh
                glBindVertexArray(mesh.getVaoId());

                // --- Iterate through instances (Transforms) using this Mesh & Atlas ---
                for (Matrix4f modelMatrix : transforms) {
                    // Upload the specific model matrix
                    defaultShaderProgram.setUniform("modelMatrix", modelMatrix);
                    defaultShaderProgram.setUniform("tintColor", defaultObjectColor);

                    // Draw the object instance
                    glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
                }
                // Note: GPU Instancing optimization could go here later
            }
        } // End loop through atlases

        glBindVertexArray(0);
        glActiveTexture(GL_TEXTURE0); // Ensure unit 0 is active before unbinding
        glBindTexture(GL_TEXTURE_2D, 0); // Unbind texture from unit 0

        // Disable attributes
        glDisableVertexAttribArray(Mesh.POSITION_VBO_ID);
        glDisableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
        glDisableVertexAttribArray(Mesh.NORMALS_VBO_ID);

        defaultShaderProgram.unbind();

        // Iterate through each mesh type
        /*for (Map.Entry<Mesh, List<ClientGameObject>> entry : objectsToRender.entrySet()) {
            Mesh mesh = entry.getKey();
            List<ClientGameObject> batch = entry.getValue();

            // Bind the VAO for this mesh
            glBindVertexArray(mesh.getVaoId());

            // VBOs / attributes
            glEnableVertexAttribArray(Mesh.POSITION_VBO_ID);
            glEnableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
            glEnableVertexAttribArray(Mesh.NORMALS_VBO_ID);

            // Iterate through each object instance using this mesh
            for (ClientGameObject gameObject : batch) {
                // Upload the model matrix for this specific object
                defaultShaderProgram.setUniform("modelMatrix", gameObject.getModelMatrix());

                // TODO: A system to apply uniforms properly, could it be an event array? (Could that be useful for us? for modders?)
                defaultShaderProgram.setUniform("tintColor", defaultObjectColor);

                // Draw call
                glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
            }

            // Unbind VAO
            glBindVertexArray(0);

            // Disable VBOs / attributes
            glDisableVertexAttribArray(Mesh.POSITION_VBO_ID);
            glDisableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
            glDisableVertexAttribArray(Mesh.NORMALS_VBO_ID);
        }

        defaultShaderProgram.unbind();*/
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