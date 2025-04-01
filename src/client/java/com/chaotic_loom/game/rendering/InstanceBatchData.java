package com.chaotic_loom.game.rendering;

import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

record InstanceBatchData(
        List<Matrix4f> modelMatrices,
        List<Vector2f> uvOffsets, // vec2(u0, v0)
        List<Vector2f> uvScales   // vec2(u1-u0, v1-v0)
        // Potentially add List<Vector3f> tintColors here too
) {
    public InstanceBatchData() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
}
