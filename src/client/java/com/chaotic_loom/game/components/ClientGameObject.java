package com.chaotic_loom.game.components;

import com.chaotic_loom.game.registries.components.Identifier;
import com.chaotic_loom.game.rendering.Mesh;
import com.chaotic_loom.game.rendering.TextureAtlasInfo;
import com.chaotic_loom.game.rendering.TextureManager;
import org.joml.*;

public class ClientGameObject extends GameObject {
    private final Mesh mesh;
    private final String texturePath;
    private final TextureAtlasInfo atlasInfo;

    // Cached model matrix
    private final Matrix4f modelMatrix;

    public ClientGameObject(Mesh mesh, String texturePath) {
        this.mesh = mesh;
        this.texturePath = texturePath;
        this.atlasInfo = TextureManager.getInstance().getTextureInfo(texturePath);
        this.modelMatrix = new Matrix4f().identity();
        recalculateMatrix(); // Calculate initial matrix
    }

    // --- Getters ---
    public Mesh getMesh() {
        return mesh;
    }
    public String getTexturePath() {
        return texturePath;
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