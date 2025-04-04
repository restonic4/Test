package com.chaotic_loom.game.core;

import com.chaotic_loom.game.rendering.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {
    // Keyboard state
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1]; // Store press state

    // Mouse state
    private final boolean[] mouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private double mouseX, mouseY;
    private double previousX, previousY;
    private double deltaX, deltaY;
    private boolean mouseInitialized = false;
    private boolean cursorDisabled = false;

    // GLFW Callback instances (to allow releasing them later)
    private GLFWKeyCallback keyCallback;
    private GLFWCursorPosCallback cursorPosCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;

    public InputManager() {
        // Initialize arrays to false
        Arrays.fill(keys, false);
        Arrays.fill(mouseButtons, false);
    }

    /**
     * Initializes the InputManager by setting up GLFW callbacks.
     * @param window The game window.
     */
    public void init(Window window) {
        long windowHandle = window.getWindowHandle();

        // --- Keyboard Callback ---
        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key >= 0 && key <= GLFW_KEY_LAST) {
                    if (action == GLFW_PRESS) {
                        keys[key] = true;
                        // Can add logic here for single-press actions if needed
                    } else if (action == GLFW_RELEASE) {
                        keys[key] = false;
                        // Can add logic here for release actions
                    }
                    // GLFW_REPEAT is ignored for simple state tracking
                }

                // Example: Close window on ESCAPE release (can be handled here or in engine)
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                    glfwSetWindowShouldClose(window, true);
                }
            }
        };
        glfwSetKeyCallback(windowHandle, keyCallback);

        // --- Mouse Position Callback ---
        cursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mouseX = xpos;
                mouseY = ypos;

                // Handle the very first mouse input to avoid large delta jump
                if (!mouseInitialized) {
                    previousX = mouseX;
                    previousY = mouseY;
                    mouseInitialized = true;
                }
            }
        };
        glfwSetCursorPosCallback(windowHandle, cursorPosCallback);

        // --- Mouse Button Callback ---
        mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button >= 0 && button <= GLFW_MOUSE_BUTTON_LAST) {
                    if (action == GLFW_PRESS) {
                        mouseButtons[button] = true;
                    } else if (action == GLFW_RELEASE) {
                        mouseButtons[button] = false;
                    }
                }
            }
        };
        glfwSetMouseButtonCallback(windowHandle, mouseButtonCallback);

        // Initialize previous positions
        // Fetch initial cursor position in case callback hasn't fired yet
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(windowHandle, x, y);
        mouseX = x[0];
        mouseY = y[0];
        previousX = mouseX;
        previousY = mouseY;
        mouseInitialized = true; // Assume initialized after explicit get

        Loggers.INPUT_MANAGER.info("Input Manager Initialized.");
    }

    /**
     * Updates the input state, calculating mouse delta.
     * Should be called once per frame after polling events and before using input state.
     */
    public void update() {
        // Calculate mouse delta for this frame
        deltaX = mouseX - previousX;
        deltaY = mouseY - previousY; // GLFW Y is top-down, OpenGL is bottom-up usually

        // Update previous position for next frame's delta calculation
        previousX = mouseX;
        previousY = mouseY;

        // Reset delta if cursor is not disabled to prevent jumps when re-entering window
        if (!cursorDisabled && (deltaX != 0 || deltaY != 0)) {
            // Maybe only reset if mouse just became visible? Needs careful handling.
            // Or simply let the game logic ignore delta when cursor is visible.
            // For now, we calculate it always. Game logic decides if to use it.
        }
    }

    // --- Query Methods ---

    /**
     * Checks if a specific keyboard key is currently held down.
     * @param keyCode The GLFW key code (e.g., GLFW_KEY_W).
     * @return true if the key is pressed, false otherwise.
     */
    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode <= GLFW_KEY_LAST) {
            return keys[keyCode];
        }
        return false;
    }

    /**
     * Checks if a specific mouse button is currently held down.
     * @param buttonCode The GLFW mouse button code (e.g., GLFW_MOUSE_BUTTON_LEFT).
     * @return true if the button is pressed, false otherwise.
     */
    public boolean isMouseButtonPressed(int buttonCode) {
        if (buttonCode >= 0 && buttonCode <= GLFW_MOUSE_BUTTON_LAST) {
            return mouseButtons[buttonCode];
        }
        return false;
    }

    /** Gets the current mouse X position relative to the window. */
    public double getMouseX() {
        return mouseX;
    }

    /** Gets the current mouse Y position relative to the window. */
    public double getMouseY() {
        return mouseY;
    }

    /** Gets the change in mouse X position since the last frame. */
    public double getDeltaX() {
        return deltaX;
    }

    /** Gets the change in mouse Y position since the last frame. */
    public double getDeltaY() {
        // Inverting deltaY is common if your camera logic expects Y-up convention
        // return -deltaY;
        return deltaY;
    }

    /** Checks if the mouse cursor is currently hidden and captured. */
    public boolean isCursorDisabled() {
        return cursorDisabled;
    }

    /**
     * Sets the cursor mode (visible/normal or hidden/captured).
     * @param disabled true to disable (hide/capture), false to enable (normal).
     * @param window The game window.
     */
    public void setCursorDisabled(boolean disabled, Window window) {
        if (cursorDisabled != disabled) {
            cursorDisabled = disabled;
            glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR,
                    disabled ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);

            // Reset delta calculation when changing modes to avoid jumps
            if (disabled) {
                // Center cursor when disabling? Optional. Helps prevent jump on first move.
                // glfwSetCursorPos(window.getWindowHandle(), window.getWidth() / 2.0, window.getHeight() / 2.0);
                // Re-fetch position after setting it
                double[] x = new double[1]; double[] y = new double[1];
                glfwGetCursorPos(window.getWindowHandle(), x, y);
                mouseX = x[0]; mouseY = y[0];
            }
            // Reset previous positions immediately when mode changes
            previousX = mouseX;
            previousY = mouseY;
            deltaX = 0;
            deltaY = 0;
            mouseInitialized = true; // Ensure it's marked initialized
        }
    }


    /**
     * Cleans up by releasing GLFW callbacks.
     * @param window The game window.
     */
    public void cleanup(Window window) {
        long windowHandle = window.getWindowHandle();

        // Set the callbacks to null in GLFW FIRST
        glfwSetKeyCallback(windowHandle, null);
        glfwSetCursorPosCallback(windowHandle, null);
        glfwSetMouseButtonCallback(windowHandle, null);

        // Then free the callback objects
        if (keyCallback != null) {
            keyCallback.free();
            keyCallback = null;
        }
        if (cursorPosCallback != null) {
            cursorPosCallback.free();
            cursorPosCallback = null;
        }
        if (mouseButtonCallback != null) {
            mouseButtonCallback.free();
            mouseButtonCallback = null;
        }

        Loggers.INPUT_MANAGER.info("Input Manager Cleaned Up.");
    }
}