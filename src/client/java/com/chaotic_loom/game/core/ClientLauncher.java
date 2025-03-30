package com.chaotic_loom.game.core;

public class ClientLauncher extends AbstractLauncher {
    public static void main(String[] args) {
        ClientEngine engine = new ClientEngine();
        launch(engine, args);
    }
}
