package com.chaotic_loom.game.world.components;

import com.chaotic_loom.game.rendering.mesh.Mesh;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;
import com.chaotic_loom.game.world.components.GameObject;
import org.joml.*;

public class ClientGameObject extends GameObject {
    private final Mesh mesh;
    private final TextureAtlasInfo atlasInfo;

    // Cached model matrix
    private final Matrix4f modelMatrix;

    public ClientGameObject(Mesh mesh, TextureAtlasInfo textureAtlasInfo) {
        this.mesh = mesh;
        this.atlasInfo = textureAtlasInfo;
        this.modelMatrix = new Matrix4f().identity();
        recalculateMatrix(); // Calculate initial matrix
    }

    // --- Getters ---
    public Mesh getMesh() {
        return mesh;
    }
    public TextureAtlasInfo getAtlasInfo() {
        return atlasInfo;
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