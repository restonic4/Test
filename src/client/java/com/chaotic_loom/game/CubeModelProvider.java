package com.chaotic_loom.game;

import com.chaotic_loom.game.rendering.TextureManager;
import com.chaotic_loom.game.rendering.components.ChunkMesher;
import com.chaotic_loom.game.rendering.mesh.Cube;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;
import com.chaotic_loom.game.world.components.Block;
import com.chaotic_loom.game.world.components.BlockInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Map;

public class CubeModelProvider implements IBlockModelProvider {
    // Singleton instance for convenience
    public static final CubeModelProvider INSTANCE = new CubeModelProvider();

    // Private constructor for singleton pattern
    private CubeModelProvider() {}

    // Rotation cache or calculation helper (optional but recommended)
    private static final Map<Block.Direction, Quaternionf> ROTATIONS = Map.of(
            Block.Direction.DOWN,  new Quaternionf().rotateX((float)Math.toRadians( 90)),
            Block.Direction.UP,    new Quaternionf().rotateX((float)Math.toRadians(-90)),
            Block.Direction.NORTH, new Quaternionf(), // Identity - No rotation
            Block.Direction.SOUTH, new Quaternionf().rotateY((float)Math.toRadians(180)),
            Block.Direction.WEST,  new Quaternionf().rotateY((float)Math.toRadians( 90)),
            Block.Direction.EAST,  new Quaternionf().rotateY((float)Math.toRadians(-90))
    );

    @Override
    public boolean addFaceGeometry(ChunkMesher.MeshBuildContext ctx, BlockInstance blockInstance, int x, int y, int z, Block.Face face) {
        Block block = blockInstance.getBlock();
        Block.Direction direction = blockInstance.getDirection();
        boolean isOpaqueFace = !block.getSettings().isTransparent();

        // --- 1. Get Texture Atlas Info ---
        TextureAtlasInfo atlasInfo = getTextureAtlasInfoForBlockFace(ctx.textureManager, block, face);
        if (atlasInfo == null) {
            System.err.printf("CubeModelProvider: Missing TextureAtlasInfo for block %s, face %s at [%d,%d,%d]. Using fallback.%n", block.getIdentifier(), face, x, y, z);
            atlasInfo = ctx.textureManager.getTextureInfo("/textures/debug_missing.png");
            if (atlasInfo == null) {
                System.err.println("CubeModelProvider: FATAL - Fallback texture '/textures/debug_missing.png' not found!");
                return false; // Critical failure
            }
        }

        // --- 2. Verify Atlas Texture ---
        if (ctx.atlasTexture == null) {
            ctx.atlasTexture = atlasInfo.atlasTexture();
        } else if (ctx.atlasTexture != atlasInfo.atlasTexture()) {
            // This should ideally not happen if TextureManager manages atlases properly
            System.err.println("CubeModelProvider: CRITICAL ERROR - Encountered multiple texture atlases! Check TextureManager setup.");
            return false;
        }


        // --- 3. Determine target lists and vertex offset ---
        List<Float> targetPositions;
        List<Float> targetUvs;
        List<Float> targetNormals;
        List<Integer> targetIndices;
        int baseVertexIndex;

        if (isOpaqueFace) {
            targetPositions = ctx.positions_opaque;
            targetUvs = ctx.uvs_opaque;
            targetNormals = ctx.normals_opaque;
            targetIndices = ctx.indices_opaque;
            baseVertexIndex = ctx.currentVertexOffset_opaque;
            ctx.currentVertexOffset_opaque += 4;
        } else {
            targetPositions = ctx.positions_transparent;
            targetUvs = ctx.uvs_transparent;
            targetNormals = ctx.normals_transparent;
            targetIndices = ctx.indices_transparent;
            baseVertexIndex = ctx.currentVertexOffset_transparent;
            ctx.currentVertexOffset_transparent += 4;
        }

        // --- 4. Get Rotation ---
        // For a standard cube, the 'direction' usually affects texture orientation,
        // *not* the geometry itself unless it's a directional block like a furnace or log.
        // Here, we'll apply the rotation for demonstration, assuming the block *should* rotate.
        // If only texture should rotate, this logic needs adjustment (likely rotating UVs).
        Quaternionf rotation = ROTATIONS.getOrDefault(direction, new Quaternionf()); // Get rotation for the direction

        // --- 5. Get Base Geometry for the Face ---
        byte faceIndex = face.getFaceIndex();
        int posStartIndex = faceIndex * 12; // 4 verts * 3 floats
        int uvStartIndex = faceIndex * 8;   // 4 verts * 2 floats
        int normStartIndex = faceIndex * 12; // 4 verts * 3 floats

        // --- 6. Add Rotated Vertices, UVs, Normals ---
        Vector4f pos = new Vector4f(0, 0, 0, 1); // Use Vector4f for matrix multiplication
        Vector3f norm = new Vector3f();

        for (int i = 0; i < 4; i++) { // For each vertex of the face
            // Base position relative to (0,0,0)
            float baseX = Cube.POSITIONS[posStartIndex + i * 3 + 0];
            float baseY = Cube.POSITIONS[posStartIndex + i * 3 + 1];
            float baseZ = Cube.POSITIONS[posStartIndex + i * 3 + 2];
            pos.set(baseX, baseY, baseZ, 1.0f);

            // Base normal
            float baseNormX = Cube.NORMALS[normStartIndex + i * 3 + 0];
            float baseNormY = Cube.NORMALS[normStartIndex + i * 3 + 1];
            float baseNormZ = Cube.NORMALS[normStartIndex + i * 3 + 2];
            norm.set(baseNormX, baseNormY, baseNormZ);

            // Apply rotation to position and normal
            rotation.transform(pos); // Rotate position (around 0,0,0)
            rotation.transform(norm); // Rotate normal
            norm.normalize(); // Ensure normal is unit length after rotation

            // Add world offset and add to list
            targetPositions.add(pos.x + x);
            targetPositions.add(pos.y + y);
            targetPositions.add(pos.z + z);

            // Add rotated normal to list
            targetNormals.add(norm.x);
            targetNormals.add(norm.y);
            targetNormals.add(norm.z);

            // UVs: Get base UV (0-1 range) and map it to the atlas sub-region
            // UVs usually aren't rotated by the model's direction unless intended.
            // If UVs need rotation based on Block.Direction, add that logic here.
            float baseU = Cube.BASE_UVS[uvStartIndex + i * 2 + 0];
            float baseV = Cube.BASE_UVS[uvStartIndex + i * 2 + 1];
            float finalU = atlasInfo.u0() + baseU * atlasInfo.getWidthUV();
            float finalV = atlasInfo.v0() + baseV * atlasInfo.getHeightUV();
            targetUvs.add(finalU);
            targetUvs.add(finalV);
        }

        // --- 7. Add Indices ---
        // Indices remain the same relative ordering for the face quad
        targetIndices.add(baseVertexIndex + 0);
        targetIndices.add(baseVertexIndex + 1);
        targetIndices.add(baseVertexIndex + 2);
        targetIndices.add(baseVertexIndex + 0);
        targetIndices.add(baseVertexIndex + 2);
        targetIndices.add(baseVertexIndex + 3);

        return true; // Success
    }


    private TextureAtlasInfo getTextureAtlasInfoForBlockFace(TextureManager textureManager, Block block, Block.Face face) {
        // This logic is specific to how textures are defined in Block.Settings
        Map<Block.Face, String> textures = block.getSettings().getFaceProperties().getTextures();
        String texturePath = textures.get(face);

        if (texturePath == null) {
            // Maybe try a default face if specific one is missing? Or handle upstream.
            System.err.printf("CubeModelProvider: Texture path is null for block %s, face %s.%n", block.getIdentifier(), face);
            return null;
        }
        return textureManager.getTextureInfo(texturePath);
    }

    @Override
    @Nullable
    public Block.Face getFaceFromNeighborOffset(int neighborDx, int neighborDy, int neighborDz) {
        // Standard cube face mapping based on neighbor offset
        if (neighborDx == 1 && neighborDy == 0 && neighborDz == 0) return Block.Face.RIGHT;   // +X
        if (neighborDx == -1 && neighborDy == 0 && neighborDz == 0) return Block.Face.LEFT;   // -X
        if (neighborDx == 0 && neighborDy == 1 && neighborDz == 0) return Block.Face.TOP;     // +Y
        if (neighborDx == 0 && neighborDy == -1 && neighborDz == 0) return Block.Face.BOTTOM; // -Y
        if (neighborDx == 0 && neighborDy == 0 && neighborDz == 1) return Block.Face.FRONT;  // +Z (Assuming +Z is Front)
        if (neighborDx == 0 && neighborDy == 0 && neighborDz == -1) return Block.Face.BACK;   // -Z (Assuming -Z is Back)
        return null; // Invalid offset
    }
}