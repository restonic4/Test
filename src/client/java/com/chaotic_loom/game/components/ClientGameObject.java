package com.chaotic_loom.game.components;

import com.chaotic_loom.game.rendering.Mesh;
import org.joml.*;

public class ClientGameObject extends GameObject {
    private final Mesh mesh;

    // Cached model matrix
    private final Matrix4f modelMatrix;

    public ClientGameObject(Mesh mesh) {
        this.mesh = mesh;
        this.modelMatrix = new Matrix4f().identity();
        recalculateMatrix(); // Calculate initial matrix
    }

    // --- Getters ---
    public Mesh getMesh() {
        return mesh;
    }

    // --- Model Matrix ---
    private void recalculateMatrix() {
        modelMatrix.identity()
                .translate(this.getTransform().getPosition())
                .rotate(this.getTransform().getRotation())
                .scale(this.getTransform().getScale());

        this.getTransform().setClean();
    }

    public Matrix4f getModelMatrix() {
        if (this.getTransform().isDirty()) {
            recalculateMatrix();
        }
        return modelMatrix;
    }
}