package com.chaotic_loom.game.world;

import com.chaotic_loom.game.rendering.TextureManager;
import com.chaotic_loom.game.rendering.components.ChunkMesher;
import com.chaotic_loom.game.rendering.mesh.Mesh;
import com.chaotic_loom.game.rendering.texture.Texture;
import com.chaotic_loom.game.world.components.Block;
import com.chaotic_loom.game.world.components.BlockInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import static com.chaotic_loom.game.core.util.SharedConstants.*;

public class ClientChunk {

    private final ChunkData chunkData; // The block data
    private final TextureManager textureManager; // Needed for remeshing
    // private final WorldAccessor world; // Needed for seamless meshing

    @Nullable private Mesh meshOpaque = null;
    @Nullable private Mesh meshTransparent = null;
    @Nullable private Texture atlasTexture = null; // Texture used by meshes

    private boolean dirty = true; // Needs remeshing initially
    private final Vector3f worldPosition; // Position for rendering transforms

    public ClientChunk(ChunkData chunkData, TextureManager textureManager /*, WorldAccessor world */) {
        this.chunkData = chunkData;
        this.textureManager = textureManager;
        // this.world = world;
        // Calculate world position based on chunk coordinates for rendering
        this.worldPosition = new Vector3f(
                chunkData.getChunkX() * CHUNK_WIDTH,
                chunkData.getChunkY() * CHUNK_HEIGHT,
                chunkData.getChunkZ() * CHUNK_DEPTH
        );
        // Initial mesh build could be triggered here or managed externally
    }

    /**
     * Updates a block based on server message and marks the chunk for remeshing.
     * @param x Local X
     * @param y Local Y
     * @param z Local Z
     * @param block New block
     */
    public void updateBlock(int x, int y, int z, BlockInstance block) {
        if (chunkData.setBlock(x, y, z, block)) {
            this.dirty = true;
            // TODO: If block is on a chunk border, mark neighbor ClientChunks as dirty too!
        }
    }

    /**
     * Rebuilds the chunk's meshes if it's marked as dirty.
     * This might be called from a separate thread pool.
     */
    public void rebuildMeshIfNeeded() {
        if (!dirty) return;

        // 1. Cleanup existing meshes (important!)
        cleanupMeshes();

        // 2. Generate new meshes
        ChunkMesher.ChunkMeshBuildResult result = ChunkMesher.generateMeshes(chunkData, textureManager /*, world */);

        // 3. Store results
        if (result != null) {
            this.meshOpaque = result.meshOpaque();
            this.meshTransparent = result.meshTransparent();
            this.atlasTexture = result.atlasTexture();
        }

        this.dirty = false; // Mark clean
    }

    /** Safely cleans up existing mesh resources. */
    private void cleanupMeshes() {
        if (meshOpaque != null) {
            meshOpaque.cleanup();
            meshOpaque = null;
        }
        if (meshTransparent != null) {
            meshTransparent.cleanup();
            meshTransparent = null;
        }
        atlasTexture = null; // Atlas is managed by TextureManager, just clear ref
    }

    /** Cleans up meshes when the chunk is unloaded. */
    public void unload() {
        cleanupMeshes();
    }

    // --- Getters ---
    public ChunkData getChunkData() { return chunkData; }
    @Nullable public Mesh getMeshOpaque() { return meshOpaque; }
    @Nullable public Mesh getMeshTransparent() { return meshTransparent; }
    @Nullable public Texture getAtlasTexture() { return atlasTexture; }
    public boolean isDirty() { return dirty; }
    public Vector3f getWorldPosition() { return worldPosition; } // For rendering transform

}