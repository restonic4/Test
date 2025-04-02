package com.chaotic_loom.game.core;

import com.chaotic_loom.game.BlockTypes;
import com.chaotic_loom.game.ChunkData;
import com.chaotic_loom.game.ClientChunk;
import com.chaotic_loom.game.components.ClientGameObject;
import com.chaotic_loom.game.core.utils.ClientConstants;
import com.chaotic_loom.game.events.WindowEvents;
import com.chaotic_loom.game.networking.ClientNetworkingContext;
import com.chaotic_loom.game.rendering.*;
import com.chaotic_loom.game.rendering.components.Camera;
import com.chaotic_loom.game.rendering.mesh.Cube;
import com.chaotic_loom.game.rendering.mesh.Mesh;
import com.chaotic_loom.game.rendering.texture.Texture;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;
import com.chaotic_loom.game.rendering.util.RenderStats;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;

public class ClientEngine extends AbstractEngine {
    private final Window window;
    private final Renderer renderer;
    private final Camera camera;
    private final InputManager inputManager;
    private final ClientNetworkingContext clientNetworkingContext;
    private final ClientTimer timer;
    private final RenderStats renderStats;
    private final TextureManager textureManager;

    private final List<ClientGameObject> gameObjects; // TEMP state
    private final Map<Texture, Map<Mesh, Map<TextureAtlasInfo, List<Matrix4f>>>> atlasRenderBatch;

    public ClientEngine() {
        super(Environment.CLIENT);

        this.window = new Window("Loading", 1280, 720);
        this.renderer = new Renderer();
        this.camera = new Camera();
        this.inputManager = new InputManager();
        this.clientNetworkingContext = new ClientNetworkingContext();
        this.timer = new ClientTimer();
        this.renderStats = new RenderStats();
        this.textureManager = new TextureManager();
        this.gameObjects = new ArrayList<>(); // TEMP state
        this.atlasRenderBatch = new HashMap<>();
    }

    @Override
    protected void init() throws Exception {
        // TODO: Should not be a throw, single-player should be a thing
        getArgsManager().throwIfMissing("username");
        getArgsManager().throwIfMissing("uuid");

        window.init();
        textureManager.bakeAtlases("textures");
        timer.init();
        renderer.init(window);
        inputManager.init(window);
        camera.setPerspective(60.0f, (float) window.getWidth() / window.getHeight(), 0.1f, 1000.0f);

        // Create sample geometry (TEMP)
        Mesh cubeMesh = Cube.createMesh();
        ClientGameObject cube1 = new ClientGameObject(cubeMesh, textureManager.getTextureInfo("/textures/stone.png"));
        cube1.getTransform().setPosition(0, 0, -2);
        gameObjects.add(cube1);
        ClientGameObject cube2 = new ClientGameObject(cubeMesh, textureManager.getTextureInfo("/textures/wood.png"));
        cube2.getTransform().setPosition(-1.5f, 0.5f, -3);
        cube2.getTransform().setScale(0.5f);
        gameObjects.add(cube2);
        ClientGameObject cube3 = new ClientGameObject(cubeMesh, textureManager.getTextureInfo("/textures/wood.png"));
        cube3.getTransform().setPosition(-3.5f, 1.5f, -4);
        cube3.getTransform().setScale(1.5f);
        gameObjects.add(cube3);

        for (int i = 0; i < 5; i++) {
            ClientGameObject cube = new ClientGameObject(cubeMesh, textureManager.getTextureInfo("/textures/dirt.png"));
            cube.getTransform().setPosition(0, 0, -4 * i);
            gameObjects.add(cube);
        }

        /*ChunkMesh.ChunkMeshBuildResult chunkBuildResult = ChunkMesh.test(textureManager);

        if (chunkBuildResult != null && chunkBuildResult.atlasTexture() != null) {
            Texture chunkAtlasTexture = chunkBuildResult.atlasTexture();
            // Use a dummy AtlasInfo just to pass the atlas texture to the renderer batching
            TextureAtlasInfo chunkAtlasInfo = new TextureAtlasInfo(chunkAtlasTexture, 0, 0, 1, 1);
            Vector3f chunkPosition = new Vector3f(5, 0, 0); // Store position

            // Create GameObject for the Opaque mesh (if it exists)
            if (chunkBuildResult.meshOpaque() != null) {
                Mesh opaqueMesh = chunkBuildResult.meshOpaque();
                ClientGameObject chunkGameObjectOpaque = new ClientGameObject(opaqueMesh, chunkAtlasInfo);
                chunkGameObjectOpaque.getTransform().setPosition(chunkPosition);
                // *** Add Opaque GO to the list *first* ***
                gameObjects.add(chunkGameObjectOpaque);
                getLogger().info("Created Opaque Chunk GameObject.");
            }

            // Create GameObject for the Transparent mesh (if it exists)
            if (chunkBuildResult.meshTransparent() != null) {
                Mesh transparentMesh = chunkBuildResult.meshTransparent();
                ClientGameObject chunkGameObjectTransparent = new ClientGameObject(transparentMesh, chunkAtlasInfo);
                chunkGameObjectTransparent.getTransform().setPosition(chunkPosition);
                // *** Add Transparent GO to the list *after* opaque ones ***
                gameObjects.add(chunkGameObjectTransparent);
                getLogger().info("Created Transparent Chunk GameObject.");
            }

        } else {
            getLogger().error("Failed to build and create chunk game object(s) for testing.");
        }*/
        int testChunkX = 1;
        int testChunkY = 0;
        int testChunkZ = 1;
        ChunkData manualChunkData = new ChunkData(testChunkX, testChunkY, testChunkZ);
        System.out.println("Created ChunkData at: " + testChunkX + ", " + testChunkY + ", " + testChunkZ);

        // 3. Populate ChunkData with blocks
        System.out.println("Populating ChunkData...");
        for (int x = 0; x < BlockTypes.CHUNK_WIDTH; x++) {
            for (int z = 0; z < BlockTypes.CHUNK_DEPTH; z++) {
                // Create a base layer
                manualChunkData.setBlock(x, 0, z, BlockTypes.BLOCK_STONE);

                // Add some features on layer 1
                if (x == 0 || x == BlockTypes.CHUNK_WIDTH - 1 || z == 0 || z == BlockTypes.CHUNK_DEPTH - 1) {
                    manualChunkData.setBlock(x, 1, z, BlockTypes.BLOCK_WOOD_LOG); // Border of logs
                } else if (x > 5 && x < 10 && z > 5 && z < 10) {
                    manualChunkData.setBlock(x, 1, z, BlockTypes.BLOCK_GLASS); // Central glass area
                } else {
                    manualChunkData.setBlock(x, 1, z, BlockTypes.BLOCK_DIRT); // Fill with dirt
                }
            }
        }
        // Add a small pillar
        manualChunkData.setBlock(2, 1, 2, BlockTypes.BLOCK_STONE);
        manualChunkData.setBlock(2, 2, 2, BlockTypes.BLOCK_STONE);
        manualChunkData.setBlock(2, 3, 2, BlockTypes.BLOCK_GLASS);
        manualChunkData.setBlock(2, 4, 2, BlockTypes.BLOCK_STONE);
        System.out.println("Population complete.");

        // 4. Create the ClientChunk wrapper
        // If your mesher needs neighbor data, you'd pass a WorldAccessor here too.
        ClientChunk manualClientChunk = new ClientChunk(manualChunkData, textureManager /*, worldAccessor */);
        System.out.println("Created ClientChunk wrapper.");

        // 5. Trigger mesh generation
        System.out.println("Requesting mesh rebuild...");
        manualClientChunk.rebuildMeshIfNeeded(); // This internally calls ChunkMesher
        System.out.println("Mesh rebuild process finished.");

        // 6. Create GameObject(s) from the result
        Texture atlas = manualClientChunk.getAtlasTexture();
        Mesh opaqueMesh = manualClientChunk.getMeshOpaque();
        Mesh transparentMesh = manualClientChunk.getMeshTransparent();
        Vector3f worldPos = manualClientChunk.getWorldPosition(); // Get calculated world pos

        if (atlas == null && (opaqueMesh != null || transparentMesh != null)) {
            System.err.println("Warning: Meshes generated but atlas texture reference is missing!");
            // Handle this case - maybe use a default atlas?
        }

        // Create placeholder AtlasInfo - only the 'atlasTexture' field really matters
        // for the existing renderer batching when using pre-meshed chunks.
        TextureAtlasInfo placeholderAtlasInfo = (atlas != null)
                ? new TextureAtlasInfo(atlas, 0, 0, 1, 1)
                : null; // Or handle error if atlas is null but meshes exist

        if (placeholderAtlasInfo != null) {
            // Add Opaque GameObject if mesh exists
            if (opaqueMesh != null) {
                ClientGameObject goOpaque = new ClientGameObject(opaqueMesh, placeholderAtlasInfo);
                goOpaque.getTransform().setPosition(worldPos);
                gameObjects.add(goOpaque); // Add opaque first
                System.out.println("Added Opaque GameObject for manual chunk.");
            } else {
                System.out.println("Manual chunk has no opaque geometry.");
            }

            // Add Transparent GameObject if mesh exists
            if (transparentMesh != null) {
                ClientGameObject goTransparent = new ClientGameObject(transparentMesh, placeholderAtlasInfo);
                goTransparent.getTransform().setPosition(worldPos);
                gameObjects.add(goTransparent); // Add transparent last
                System.out.println("Added Transparent GameObject for manual chunk.");
            } else {
                System.out.println("Manual chunk has no transparent geometry.");
            }
        } else if (opaqueMesh != null || transparentMesh != null) {
            System.err.println("Cannot create GameObjects for manual chunk - Atlas Texture is missing!");
        } else {
            System.out.println("Manual chunk resulted in no geometry.");
        }
        System.out.println("--- Manual Test Chunk Creation Finished ---");


        //TempServer.joinServer(this);

        // Modify viewport on window modification
        WindowEvents.RESIZE.register((window1, width, height) -> {
            // Update camera's aspect ratio which recalculates projection
            camera.setAspectRatio((float)window.getWidth() / window.getHeight());
        });

        getLogger().info("Client Engine Initialized.");
    }

    @Override
    protected void gameLoop() throws Exception {
        float elapsedTime;

        while (!window.isCloseRequested()) {
            // Get time delta for this frame
            elapsedTime = timer.getElapsedTime();

            // --- Input Processing ---
            // Process continuous input (movement keys)
            processInput(elapsedTime);

            // --- Client Update ---
            // TODO: Update client-side logic (animations, interpolation, prediction)
            inputManager.update();

            // --- Rendering ---
            render();
            timer.frameRendered(); // Update FPS counter

            // --- Window update ---
            if (!window.isVSync()) {
                sync(); // Syncing (Manual FPS Cap)
            }

            window.update();
        }
    }

    private void processInput(float elapsedTime) {
        // --- Camera Movement ---
        float moveSpeed = 5.0f * elapsedTime;
        Vector3f deltaPos = new Vector3f();
        Vector3f currentPos = camera.getPosition(new Vector3f()); // Get current position
        Vector3f forwardDir = camera.getDirection(new Vector3f());
        Vector3f rightDir = camera.getRight(new Vector3f());
        Vector3f worldUp = new Vector3f(0, 1, 0); // Or use camera.getUp() for different behavior

        if (inputManager.isKeyPressed(GLFW_KEY_W)) deltaPos.add(forwardDir);
        if (inputManager.isKeyPressed(GLFW_KEY_S)) deltaPos.sub(forwardDir);
        if (inputManager.isKeyPressed(GLFW_KEY_A)) deltaPos.sub(rightDir);
        if (inputManager.isKeyPressed(GLFW_KEY_D)) deltaPos.add(rightDir);
        if (inputManager.isKeyPressed(GLFW_KEY_SPACE)) deltaPos.add(worldUp); // Move along world Y
        if (inputManager.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) deltaPos.sub(worldUp); // Move along world Y
        if (inputManager.isKeyPressed(GLFW_KEY_X)) inputManager.setCursorDisabled(!inputManager.isCursorDisabled(), window);

        // Normalize delta if moving diagonally to prevent faster speed
        if (deltaPos.lengthSquared() > 0) {
            deltaPos.normalize().mul(moveSpeed);
            camera.setPosition(currentPos.add(deltaPos)); // Set new position
        }


        // --- Camera Look ---
        float mouseSensitivity = 0.1f;
        if (inputManager.isCursorDisabled()) {
            double deltaX = inputManager.getDeltaX();
            double deltaY = inputManager.getDeltaY();

            if (Math.abs(deltaX) > 0.01) { // Add small deadzone
                camera.yaw((float) Math.toRadians(-deltaX * mouseSensitivity)); // Yaw affects direction+up
            }
            if (Math.abs(deltaY) > 0.01) {
                camera.pitch((float) Math.toRadians(-deltaY * mouseSensitivity)); // Pitch affects direction+up
            }
        }
    }

    private void prepareRenderBatch() {
        atlasRenderBatch.clear();
        renderStats.resetFrame();

        // Prepare batch
        for (ClientGameObject go : gameObjects) {
            Mesh mesh = go.getMesh();
            TextureAtlasInfo atlasInfo = go.getAtlasInfo();

            // Validate necessary data
            if (mesh == null || atlasInfo == null || atlasInfo.atlasTexture() == null) {
                if (atlasInfo == null) getLogger().warn("GameObject missing AtlasInfo, skipping render.");
                continue;
            }

            Texture atlasTexture = atlasInfo.atlasTexture();

            // Populate the 3-level batch structure:
            atlasRenderBatch
                    .computeIfAbsent(atlasTexture, k -> new HashMap<>())    // Level 1: Atlas Texture
                    .computeIfAbsent(mesh, k -> new HashMap<>())    // Level 2: Mesh
                    .computeIfAbsent(atlasInfo, k -> new ArrayList<>())     // Level 3: AtlasInfo (UV region)
                    .add(go.getModelMatrix());      // Add instance matrix to the list
        }
    }

    private void render() {
        prepareRenderBatch();

        renderer.render(window, camera, atlasRenderBatch, renderStats);

        if (renderStats.getTotalFrames() % 60 == 0) {
            //getLogger().info(renderStats.getSummary());
        }
    }

    private void sync() {
        float loopSlot = 1f / ClientConstants.TARGET_FPS;
        double targetTime = lastSyncTime + loopSlot;

        while (timer.getTime() < targetTime - 0.001) { // Sleep until slightly before target
            try { Thread.sleep(1); } catch (InterruptedException ignore) {Thread.currentThread().interrupt(); break;}
        }
        // Busy wait for the last moment for higher precision
        // while (timer.getTime() < targetTime) { Thread.yield(); }

        lastSyncTime = timer.getTime(); // Track when sync finished
    }
    private double lastSyncTime = 0;


    @Override
    protected void cleanup() {
        getLogger().info("Cleaning up client engine...");

        getNetworkingManager().cleanup();
        renderer.cleanup();

        textureManager.cleanup();

        // Mesh cleanup (needs proper management)
        Set<Mesh> uniqueMeshes = new HashSet<>();
        for (ClientGameObject go : gameObjects) { uniqueMeshes.add(go.getMesh()); }
        getLogger().info("Cleaning up {} unique meshes...", uniqueMeshes.size());
        for (Mesh mesh : uniqueMeshes) { if(mesh != null) mesh.cleanup(); }

        inputManager.cleanup(window);
        window.cleanup();

        getLogger().info("Client engine cleanup complete.");
    }

    @Override
    protected Timer getTimer() {
        return this.timer;
    }

    public ClientNetworkingContext getClientNetworkingContext() {
        return clientNetworkingContext;
    }
}