package com.chaotic_loom.game.rendering;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
        defaultShaderProgram.createUniform("objectColor");

        // Enable Depth Testing (Important for 3D)
        glEnable(GL_DEPTH_TEST);

        // Enable backface culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    public void render(Window window, Camera camera, Map<Mesh, List<ClientGameObject>> objectsToRender) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (window.isResized()) {
            glViewport(0, 0, window.getWidth(), window.getHeight());

            // Update camera's aspect ratio which recalculates projection
            camera.setAspectRatio((float)window.getWidth() / window.getHeight());
            camera.recalculateProjectionMatrix();
        }

        defaultShaderProgram.bind();

        // Upload camera matrices
        defaultShaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        defaultShaderProgram.setUniform("viewMatrix", camera.getViewMatrix());

        // Iterate through each mesh type
        for (Map.Entry<Mesh, List<ClientGameObject>> entry : objectsToRender.entrySet()) {
            Mesh mesh = entry.getKey();
            List<ClientGameObject> batch = entry.getValue();

            // Bind the VAO for this mesh
            glBindVertexArray(mesh.getVaoId());
            // Assuming locations 0, 1, 2 were enabled during mesh creation
            // If you manage attribute enabling/disabling per mesh:
            // glEnableVertexAttribArray(0);
            // if (mesh has tex coords) glEnableVertexAttribArray(1);
            // if (mesh has normals) glEnableVertexAttribArray(2);


            // Iterate through each object instance using this mesh
            for (ClientGameObject gameObject : batch) {
                // Upload the model matrix for this specific object
                defaultShaderProgram.setUniform("modelMatrix", gameObject.getModelMatrix());

                // Set object-specific uniforms (e.g., color)
                // For now, use a default color. Later, this could come from the GameObject.
                defaultShaderProgram.setUniform("objectColor", defaultObjectColor);


                // Issue the draw call
                if (mesh.isIndexed()) {
                    // Draw using indices (EBO)
                    glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
                } else {
                    // Draw using raw vertices (VBO only)
                    glDrawArrays(GL_TRIANGLES, 0, mesh.getVertexCount());
                }
            }

            // Unbind VAO (optional if next loop iteration binds another one)
            // Good practice if mixing with other rendering techniques
            glBindVertexArray(0);

            // Disable attributes if they were enabled here
            // glDisableVertexAttribArray(0); ...
        }

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