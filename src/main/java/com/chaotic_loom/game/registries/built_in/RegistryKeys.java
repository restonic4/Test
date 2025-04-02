package com.chaotic_loom.game.registries.built_in;

import com.chaotic_loom.game.world.components.Block;
import com.chaotic_loom.game.networking.components.Packet;
import com.chaotic_loom.game.registries.components.RegistryKey;

import java.util.HashMap;

public abstract class RegistryKeys {
    private static final HashMap<String, RegistryKey<?>> keys = new HashMap<>();

    public static final RegistryKey<Packet> PACKETS = registerKey("packets");
    public static final RegistryKey<Block> BLOCK = registerKey("block");

    private static <T> RegistryKey<T> registerKey(String key) {
        if (keys.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate registry key registration attempt: " + key);
        }

        RegistryKey<T> registryKey = new RegistryKey<>(key);
        keys.put(registryKey.key(), registryKey);

        return registryKey;
    }

    @SuppressWarnings("unchecked")
    public static <T> RegistryKey<T> getRegistryKey(String key) {
        return (RegistryKey<T>) keys.get(key);
    }
}
