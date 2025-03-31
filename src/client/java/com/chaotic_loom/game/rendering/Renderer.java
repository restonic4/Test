package com.chaotic_loom.game.rendering;

import java.util.List;
import java.util.Map;

import com.chaotic_loom.game.events.WindowEvents;
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

            // VBOs / attributes
            glEnableVertexAttribArray(Mesh.POSITION_VBO_ID);
            glEnableVertexAttribArray(Mesh.TEXTURE_COORDS_VBO_ID);
            glEnableVertexAttribArray(Mesh.NORMALS_VBO_ID);

            // Iterate through each object instance using this mesh
            for (ClientGameObject gameObject : batch) {
                // Upload the model matrix for this specific object
                defaultShaderProgram.setUniform("modelMatrix", gameObject.getModelMatrix());

                // TODO: A system to apply uniforms properly, could it be an event array? (Could that be useful for us? for modders?)
                defaultShaderProgram.setUniform("objectColor", defaultObjectColor);

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