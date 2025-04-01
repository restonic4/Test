package com.chaotic_loom.game;

import org.joml.Vector3ic;

public class ServerChunk extends CommonChunk {

    // Placeholder for server-specific data
    private boolean collisionShapeDirty = true;
    // private CollisionShape chunkCollisionShape;

    public ServerChunk(IChunkAccess worldAccess, Vector3ic chunkPos) {
        super(worldAccess, chunkPos);
    }

    @Override
    public void markDirty() {
        // Mark collision shape dirty when blocks change
        this.collisionShapeDirty = true;
        // Server doesn't have meshes, so no mesh dirty flag needed here.
    }

    @Override
    public void updateTick() {
        // Server-side chunk logic, e.g., random block ticks, fluid simulation
        // if (collisionShapeDirty) {
        //     recalculateCollisionShape();
        // }
    }

    @Override
    public void cleanup() {
        // Cleanup server-specific resources if any (e.g., remove from physics world)
    }

    // Placeholder for server-side collision shape generation
    private void recalculateCollisionShape() {
        // Iterate through blockIDs and build/update appropriate physics shape
        // ... implementation ...
        this.collisionShapeDirty = false;
        // System.out.println("Recalculated collision for chunk: " + chunkPos);
    }

    // public CollisionShape getCollisionShape() {
    //     if (collisionShapeDirty) recalculateCollisionShape();
    //     return chunkCollisionShape;
    // }

}