package com.chaotic_loom.game.world.components;

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