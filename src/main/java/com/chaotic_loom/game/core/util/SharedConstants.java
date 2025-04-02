package com.chaotic_loom.game.core.util;

public abstract class SharedConstants {
    public static final String NAMESPACE = "vanilla";

    public static final String VALID_NAMESPACE_CHARS = "_-abcdefghijklmnopqrstuvwxyz0123456789.";
    public static final String VALID_PATH_CHARS = VALID_NAMESPACE_CHARS + "/";

    public static final int CHUNK_WIDTH = 16;
    public static final int CHUNK_HEIGHT = 16;
    public static final int CHUNK_DEPTH = 16;
}
