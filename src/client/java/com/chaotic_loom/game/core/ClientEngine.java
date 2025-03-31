package com.chaotic_loom.game.core;

import com.chaotic_loom.game.components.ClientGameObject;
import com.chaotic_loom.game.core.utils.ClientConstants;
import com.chaotic_loom.game.core.utils.TempServer;
import com.chaotic_loom.game.events.WindowEvents;
import com.chaotic_loom.game.networking.ClientNetworkingContext;
import com.chaotic_loom.game.rendering.*;
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

    private final List<ClientGameObject> gameObjects; // TEMP state
    private final Map<Mesh, List<ClientGameObject>> objectsToRender; // TEMP state

    public ClientEngine() {
        super(Environment.CLIENT);

        this.window = new Window("Loading", 1280, 720);
        this.renderer = new Renderer();
        this.camera = new Camera();
        this.inputManager = new InputManager();
        this.clientNetworkingContext = new ClientNetworkingContext();
        this.timer = new ClientTimer();
        this.gameObjects = new ArrayList<>(); // TEMP state
        this.objectsToRender = new HashMap<>(); // TEMP state
    }

    @Override
    protected void init() throws Exception {
        // TODO: Should not be a throw, single-player should be a thing
        getArgsManager().throwIfMissing("username");
        getArgsManager().throwIfMissing("uuid");

        window.init();
        timer.init();
        renderer.init(window);
        inputManager.init(window);
        camera.setPerspective(60.0f, (float) window.getWidth() / window.getHeight(), 0.1f, 1000.0f);

        // Create sample geometry (TEMP)
        Mesh cubeMesh = Cube.createMesh();
        ClientGameObject cube1 = new ClientGameObject(cubeMesh);
        cube1.getTransform().setPosition(0, 0, -2);
        gameObjects.add(cube1);
        ClientGameObject cube2 = new ClientGameObject(cubeMesh);
        cube2.getTransform().setPosition(-1.5f, 0.5f, -3);
        cube2.getTransform().setScale(0.5f);
        gameObjects.add(cube2);

        TempServer.joinServer(this);

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

    private void render() {
        objectsToRender.clear();

        // We create batches of the same mesh. This groups all the GameObjects using the same mesh on a batch. 1 batch = 1 draw call.
        for (ClientGameObject go : gameObjects) { // Using TEMP list
            Mesh mesh = go.getMesh();
            if (mesh == null) continue;

            // computeIfAbsent = Looks if a list already exists for that mesh, if it does, it returns a list, if not, the lambda expression is executed.
            // (I tend to forget this because me dumb, @restonic4)
            List<ClientGameObject> batch = objectsToRender.computeIfAbsent(mesh, k -> new ArrayList<>());
            batch.add(go);
        }

        renderer.render(window, camera, objectsToRender);
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