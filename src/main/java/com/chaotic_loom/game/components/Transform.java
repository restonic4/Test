package com.chaotic_loom.game.components;

import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Transform {
    private final Vector3f position;
    private final Quaternionf rotation;
    private final Vector3f scale;

    // Reusable temporary quaternion to avoid allocations in rotation methods
    private final Quaternionf tempRotation = new Quaternionf();

    private boolean dirty = true;

    public Transform() {
        this.position = new Vector3f();
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1, 1, 1);
    }

    public Transform(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1, 1, 1);
    }

    public Transform(Vector3f position) {
        this.position = position;
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1, 1, 1);
    }

    public Transform(Vector3f position, Quaternionf rotation, Vector3f scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    // Getters

    public Vector3f getPosition() {
        return this.position;
    }

    public Vector3f getScale() {
        return this.scale;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    // Setters

    public void setPosition(Vector3f position) {
        this.position.set(position.x, position.y, position.z);
        setDirty();
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        setDirty();
    }

    public void setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        setDirty();
    }

    // Convenience method for Euler angles (use with caution - gimbal lock)
    public void setRotation(float angleX, float angleY, float angleZ) {
        this.rotation.identity().rotateXYZ(org.joml.Math.toRadians(angleX), org.joml.Math.toRadians(angleY), Math.toRadians(angleZ));
        setDirty();
    }

    public void setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
        setDirty();
    }

    public void setScale(float scale) {
        this.setScale(scale, scale, scale);
    }

    public void translate(float dx, float dy, float dz) {
        this.position.add(dx, dy, dz);
        setDirty();
    }

    public void rotateLocal(float angle, float axisX, float axisY, float axisZ) {
        float radAngle = Math.toRadians(angle);
        tempRotation.identity().rotateAxis(radAngle, axisX, axisY, axisZ);

        this.rotation.mul(tempRotation, this.rotation);
        setDirty();
    }

    public void rotateGlobal(float angle, float axisX, float axisY, float axisZ) {
        float radAngle = Math.toRadians(angle);
        tempRotation.identity().rotateAxis(radAngle, axisX, axisY, axisZ);

        this.rotation.premul(tempRotation, this.rotation);
        setDirty();
    }

    // Dirty

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        this.dirty = true;
    }

    public void setClean() {
        this.dirty = false;
    }
}
