package com.chaotic_loom.game.rendering.components; // Or your rendering package

import org.joml.*;
import org.joml.Math;

public class Camera {
    // Core State
    private final Vector3f position;
    private final Vector3f direction; // Normalized direction vector
    private final Vector3f up;        // Normalized up vector, orthogonal to direction

    // Calculated Matrices
    private final Matrix4f viewMatrix;
    private final Matrix4f projectionMatrix;

    // Projection Parameters
    private float fovRadians;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    // Temporary vectors to avoid allocations in calculations
    private final Vector3f tempVec = new Vector3f();
    private final Vector3f rightVec = new Vector3f();
    // Removed tempAxisAngle as it wasn't used in the provided code. Can be added back if needed.

    public Camera(Vector3f position, Vector3f direction, Vector3f up) {
        this.position = new Vector3f(position); // Use copies
        this.direction = new Vector3f(direction).normalize();
        this.up = new Vector3f(up).normalize();
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();

        recalculateViewMatrix();
    }

    public Camera() {
        this(new Vector3f(0, 0, 3), new Vector3f(0, 0, -1), new Vector3f(0, 1, 0));
        // Default perspective
        setPerspective(60.0f, 16.0f / 9.0f, 0.1f, 1000.0f);
    }

    // --- Matrix Recalculation ---
    private void recalculateViewMatrix() {
        // State is assumed to be valid (normalized, orthogonal) due to calls in setters/modifiers
        Vector3f target = tempVec.set(position).add(direction);
        viewMatrix.identity().lookAt(position, target, up);
    }

    private void recalculateProjectionMatrix() {
        if (aspectRatio <= 0) aspectRatio = 1.0f; // Avoid division by zero
        projectionMatrix.identity().perspective(fovRadians, aspectRatio, zNear, zFar);
    }


    // --- Setters for Core State ---

    /** Sets the absolute position of the camera. */
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        recalculateViewMatrix();
    }

    /** Sets the absolute position of the camera. */
    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition);
        recalculateViewMatrix();
    }

    /** Sets the absolute direction the camera is looking. Up vector is orthogonalized. */
    public void setDirection(float x, float y, float z) {
        this.direction.set(x, y, z).normalize();

        recalculateViewMatrix();
    }

    /** Sets the absolute direction the camera is looking. Up vector is orthogonalized. */
    public void setDirection(Vector3f newDirection) {
        this.direction.set(newDirection).normalize();

        recalculateViewMatrix();
    }

    /** Sets the camera's 'up' direction hint. It will be orthogonalized relative to the current direction. */
    public void setUp(float x, float y, float z) {
        this.up.set(x, y, z); // Set the desired 'up' hint

        recalculateViewMatrix();
    }

    /** Sets the camera's 'up' direction hint. It will be orthogonalized relative to the current direction. */
    public void setUp(Vector3f newUp) {
        this.up.set(newUp); // Set the desired 'up' hint

        recalculateViewMatrix();
    }


    /** Sets the camera orientation to look at a specific target point from its current position. Uses world Y up as initial hint for 'up'. */
    public void lookAt(Vector3f target) {
        target.sub(position, direction).normalize(); // Calculate new direction
        // Use world Y as a hint for the 'up' vector.
        this.up.set(0, 1, 0);

        recalculateViewMatrix();
    }

    /** Sets the camera orientation to look at a specific target point from its current position, providing an explicit 'up' direction hint. */
    public void lookAt(Vector3f target, Vector3f upHint) {
        target.sub(position, direction).normalize(); // Calculate new direction
        this.up.set(upHint); // Set the desired 'up' hint

        recalculateViewMatrix();
    }

    /** Sets the camera orientation using a Quaternion. */
    public void setRotation(Quaternionfc rotation) { // Use fc for read-only parameter
        // Define base vectors (local camera space: -Z forward, +Y up)
        // No memory leak: these are stack-allocated or static if made final static
        Vector3f baseDirection = new Vector3f(0, 0, -1);
        Vector3f baseUp = new Vector3f(0, 1, 0);

        // Rotate base vectors by the quaternion to get new world orientation
        rotation.transform(baseDirection, this.direction);
        rotation.transform(baseUp, this.up);

        // Normalize (likely redundant for unit quaternions, but safe)
        this.direction.normalize();
        this.up.normalize();

        // No need to validate here if the quaternion represents a valid rotation
        recalculateViewMatrix();
    }


    // --- Setters for Projection ---

    /** Sets the perspective projection parameters. */
    public void setPerspective(float fovDegrees, float aspectRatio, float zNear, float zFar) {
        this.fovRadians = (float) Math.toRadians(fovDegrees);
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
        recalculateProjectionMatrix();
    }

    /** Sets the Field of View (FOV) in degrees. */
    public void setFov(float fovDegrees) {
        this.fovRadians = (float) Math.toRadians(fovDegrees);
        recalculateProjectionMatrix();
    }

    /** Updates the aspect ratio. */
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        recalculateProjectionMatrix();
    }


    // --- Orientation Modifiers (Apply Deltas) ---

    /** Rotates the camera around its current 'up' vector (Yaw). */
    public void yaw(float angleRad) {
        // Rotate direction vector around the current 'up' vector
        // The 'up' vector itself doesn't change in a pure yaw relative to current orientation
        direction.rotateAxis(angleRad, up.x, up.y, up.z).normalize();
        // 'up' remains orthogonal because direction rotated in the plane perpendicular to 'up'
        recalculateViewMatrix();
    }

    /** Rotates the camera around its current 'right' vector (Pitch). NO CLAMPING. */
    public void pitch(float angleRad) {
        // Calculate the current 'right' vector
        direction.cross(up, rightVec).normalize();

        // Rotate both 'direction' and 'up' vectors around the 'right' vector
        direction.rotateAxis(angleRad, rightVec.x, rightVec.y, rightVec.z).normalize();
        up.rotateAxis(angleRad, rightVec.x, rightVec.y, rightVec.z).normalize();

        // The vectors should remain orthogonal after rotating both around their cross product
        // No clamping is applied.
        recalculateViewMatrix();
    }

    /** Rotates the camera around its current 'direction' vector (Roll). */
    public void roll(float angleRad) {
        // Rotate the 'up' vector around the 'direction' vector
        // The 'direction' vector remains unchanged in a pure roll
        up.rotateAxis(angleRad, direction.x, direction.y, direction.z).normalize();
        // 'direction' remains orthogonal to the new 'up'
        recalculateViewMatrix();
    }

    /** Applies a rotation delta defined by a Quaternion. Multiplies the current orientation. */
    public void rotate(Quaternionfc deltaRotation) { // Use fc for read-only parameter
        // Rotate existing direction and up vectors by the delta rotation
        deltaRotation.transform(this.direction);
        deltaRotation.transform(this.up);

        // Normalize results
        this.direction.normalize();
        this.up.normalize();

        // No need to validate orthogonality if deltaRotation is a valid rotation
        recalculateViewMatrix();
    }


    // --- Getters ---

    /** Gets the calculated view matrix. */
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    /** Gets the calculated projection matrix. */
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    /** Gets the camera's current position (copy). */
    public Vector3f getPosition(Vector3f dest) {
        return dest.set(this.position);
    }

    /** Gets the camera's current normalized direction vector (copy). */
    public Vector3f getDirection(Vector3f dest) {
        return dest.set(this.direction);
    }

    /** Gets the camera's current normalized up vector (copy). */
    public Vector3f getUp(Vector3f dest) {
        return dest.set(this.up);
    }

    /** Gets the camera's current normalized right vector (calculated, copy). */
    public Vector3f getRight(Vector3f dest) {
        // Ensure up and direction are valid before calculating right
        // validateState(); // Might be redundant if always validated on change
        return direction.cross(up, dest).normalize();
    }

    /** Gets the Field of View (FOV) in degrees. */
    public float getFovDegrees() {
        return (float) Math.toDegrees(this.fovRadians);
    }

    /** Gets the current aspect ratio. */
    public float getAspectRatio() {
        return aspectRatio;
    }

    /** Gets the near clipping plane distance. */
    public float getNearPlane() {
        return zNear;
    }

    /** Gets the far clipping plane distance. */
    public float getFarPlane() {
        return zFar;
    }
}