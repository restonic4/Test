package com.chaotic_loom.game.core;

public class ServerLauncher extends AbstractLauncher {
    public static void main(String[] args) {
        ServerEngine serverEngine = new ServerEngine();
        launch(serverEngine, args);
    }
}
