package com.chaotic_loom.game.registries.components;

public record RegistryKey<T>(String key) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegistryKey<?> that = (RegistryKey<?>) o;
        return key.equals(that.key);
    }
}
