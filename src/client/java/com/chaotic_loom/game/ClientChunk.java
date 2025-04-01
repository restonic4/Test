package com.chaotic_loom.game;

import com.chaotic_loom.game.rendering.Mesh;
import com.chaotic_loom.game.rendering.Texture;
import com.chaotic_loom.game.rendering.TextureAtlasInfo;
import com.chaotic_loom.game.rendering.TextureManager;
import org.joml.Vector3ic;

import java.util.ArrayList;
import java.util.List;

import static com.chaotic_loom.game.ChunkConstants.*;

public class ClientChunk extends CommonChunk {
    private transient Mesh mesh;
    private transient boolean dirty = true;
    private transient boolean meshGenScheduled = false; // Flag for async meshing later
    private transient Texture atlasTextureRef; // Atlas texture used by this mesh

    public ClientChunk(IChunkAccess worldAccess, Vector3ic chunkPos) {
        super(worldAccess, chunkPos);
    }

    @Override
    public void markDirty() {
        this.dirty = true;
        // In async system: if (!meshGenScheduled) { scheduleMeshGeneration(); }
    }

    @Override
    public void updateTick() {
        // Client chunks typically don't have tick updates unless for animations
    }

    @Override
    public void cleanup() {
        cleanupMesh();
    }

    /** Gets the renderable mesh, potentially triggering generation if dirty. */
    public Mesh getMesh() {
        // Simple synchronous generation for now
        if (dirty && !meshGenScheduled) {
            generateMesh();
        }
        return mesh;
    }

    public Texture getAtlasTextureRef() {
        // Ensure mesh is generated if needed before returning ref
        getMesh(); // Trigger generation if dirty
        return atlasTextureRef;
    }


    private void cleanupMesh() {
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
        // Reset flags, atlas ref might become invalid if blocks change significantly
        this.atlasTextureRef = null;
        this.dirty = true; // Mark dirty after cleanup
        this.meshGenScheduled = false;
    }


    // --- Meshing Logic (Moved from old Chunk) ---

    private void generateMesh() {
        // if (worldAccess.getEnvironment() != Environment.CLIENT) return; // Redundant check maybe

        this.meshGenScheduled = true; // Indicate generation started/scheduled
        this.atlasTextureRef = null; // Reset atlas ref

        // System.out.println("Generating mesh for client chunk: " + chunkPos); // Debug

        List<Float> positions = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int vertexIndexOffset = 0;
        TextureManager texManager = TextureManager.getInstance(); // Assumes Singleton access

        for (int ly = 0; ly < CHUNK_SIZE_Y; ly++) { // Iterate Y outer for potential cache benefits?
            for (int lz = 0; lz < CHUNK_SIZE_Z; lz++) {
                for (int lx = 0; lx < CHUNK_SIZE_X; lx++) {
                    // Use local block ID for efficiency inside the core loop
                    byte currentBlockID = this.blockIDs[getIndex(lx, ly, lz)];
                    BlockType currentBlock = BlockType.fromID(currentBlockID);

                    if (!currentBlock.isSolid()) {
                        continue; // Skip air blocks
                    }

                    // Check 6 neighbors using getBlockTypeWithNeighbors which uses worldAccess
                    checkFace(lx, ly, lz, currentBlock, lx + 1, ly, lz, positions, normals, texCoords, indices, vertexIndexOffset, BlockFace.EAST, texManager);
                    checkFace(lx, ly, lz, currentBlock, lx - 1, ly, lz, positions, normals, texCoords, indices, vertexIndexOffset, BlockFace.WEST, texManager);
                    checkFace(lx, ly, lz, currentBlock, lx, ly + 1, lz, positions, normals, texCoords, indices, vertexIndexOffset, BlockFace.UP, texManager);
                    checkFace(lx, ly, lz, currentBlock, lx, ly - 1, lz, positions, normals, texCoords, indices, vertexIndexOffset, BlockFace.DOWN, texManager);
                    checkFace(lx, ly, lz, currentBlock, lx, ly, lz + 1, positions, normals, texCoords, indices, vertexIndexOffset, BlockFace.SOUTH, texManager);
                    checkFace(lx, ly, lz, currentBlock, lx, ly, lz - 1, positions, normals, texCoords, indices, vertexIndexOffset, BlockFace.NORTH, texManager);

                    // Update index offset based on vertices actually added in checkFace calls
                    vertexIndexOffset = positions.size() / 3; // Recalculate based on list size
                }
            }
        }

        // --- Create/Update Mesh ---
        if (!positions.isEmpty()) {
            float[] posArr = toFloatArray(positions);
            float[] normArr = toFloatArray(normals);
            float[] tcArr = toFloatArray(texCoords);
            int[] idxArr = toIntArray(indices);

            if (this.mesh != null) this.mesh.cleanup(); // Simple cleanup/recreate
            this.mesh = new Mesh(posArr, tcArr, normArr, idxArr); // Mesh constructor needs client context

            if (this.atlasTextureRef == null && tcArr.length > 0) {
                System.err.println("Warning: Chunk " + chunkPos + " mesh generated but no atlas reference set!");
                // Attempt fallback: Get atlas from first valid texture path used? Complex.
            }
        } else { // No faces generated
            if (this.mesh != null) this.mesh.cleanup();
            this.mesh = null;
            this.atlasTextureRef = null;
        }
        dirty = false;
        meshGenScheduled = false;
    }

    private void checkFace(int lx, int ly, int lz, BlockType currentBlock,
                           int nx, int ny, int nz, // Neighbor coords (local)
                           List<Float> positions, List<Float> normals, List<Float> texCoords, List<Integer> indices,
                           int vertexIndexOffset, BlockFace face, TextureManager texManager)
    {
        // Use getBlockTypeWithNeighbors for boundary checks
        BlockType neighborBlock = getBlockTypeWithNeighbors(nx, ny, nz);

        if (!neighborBlock.isSolid() || (neighborBlock.isTransparent() && !currentBlock.isTransparent())) {
            String texturePath = getTexturePathForBlockFace(currentBlock, face);
            TextureAtlasInfo texInfo = texManager.getTextureInfo(texturePath);

            // If this is the first face being added, store its atlas texture
            if (texInfo != null && this.atlasTextureRef == null) {
                this.atlasTextureRef = texInfo.atlasTexture;
            } else if (texInfo != null && texInfo.atlasTexture != this.atlasTextureRef) {
                // Error/Warning: Faces in the same chunk mesh mapping to different atlases!
                // This indicates a problem with packing or texture assignment.
                System.err.printf("Inconsistent atlas textures in chunk %s! Face wants %s (%s), chunk has %s%n",
                        chunkPos.toString(), texturePath, texInfo.atlasTexture.getTextureId(), this.atlasTextureRef.getTextureId());
                // Use the chunk's primary atlas or fallback UVs for this face.
                // For now, we'll proceed using the potentially 'wrong' texInfo for UV calculation,
                // but rendering might break if the wrong atlas texture is bound later.
            }

            addFace(lx, ly, lz, face, positions, normals, texCoords, indices, vertexIndexOffset, texInfo);
        }
    }

    // addFace logic remains the same (calculates UVs based on passed texInfo)
    private void addFace(int lx, int ly, int lz, BlockFace face,
                         List<Float> positions, List<Float> normals, List<Float> texCoords, List<Integer> indices,
                         int vertexIndexOffset, TextureAtlasInfo texInfo) { /* ... same as before ... */ }

    // getTexturePathForBlockFace remains the same
    private String getTexturePathForBlockFace(BlockType blockType, BlockFace face) { /* ... same as before ... */ }

    // List to Array Helpers remain the same
    private static float[] toFloatArray(List<Float> list) { /* ... */ }
    private static int[] toIntArray(List<Integer> list) { /* ... */ }

    // BlockFace enum remains the same
    private enum BlockFace { /* ... */ }

}