package com.chaotic_loom.game.networking.components;

import java.util.UUID;

public record User(UUID uuid, String username) {
    @Override
    public String toString() {
        return username + " (" + uuid + ")";
    }
}
