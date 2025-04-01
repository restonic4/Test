package com.chaotic_loom.game.rendering;

import org.joml.Matrix4f;

public record InstanceData(Matrix4f modelMatrix, TextureAtlasInfo atlasInfo) {
    // Holds data needed just before the draw call for a specific instance
}