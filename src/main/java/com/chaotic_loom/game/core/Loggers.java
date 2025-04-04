package com.chaotic_loom.game.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Loggers {
    // Client
    public static final Logger WINDOW = LogManager.getLogger("Window");
    public static final Logger RENDERER = LogManager.getLogger("Renderer");
    public static final Logger TEXTURE_MANAGER = LogManager.getLogger("TextureManager");
    public static final Logger INPUT_MANAGER = LogManager.getLogger("InputManager");

    // Server

    // Common
    public static final Logger LAUNCHER = LogManager.getLogger("Launcher");
    public static final Logger REGISTRY = LogManager.getLogger("Registry");
    public static final Logger NETWORKING = LogManager.getLogger("Networking");
    public static final Logger CHUNK = LogManager.getLogger("Chunk");
    public static final Logger OTHER = LogManager.getLogger("Other");
}
