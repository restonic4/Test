package com.chaotic_loom.game.components;

import org.joml.*;

public class GameObject {

    private final Transform transform;

    public GameObject() {
        this.transform = new Transform();
    }

    // --- Getters ---

    public Transform getTransform() {
        return transform;
    }
}