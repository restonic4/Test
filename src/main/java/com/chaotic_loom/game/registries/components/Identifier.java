package com.chaotic_loom.game.registries.components;

import com.chaotic_loom.game.core.util.SharedConstants;

import java.util.Objects;

public class Identifier {
    private String namespace;
    private String id;

    private RegistryKey<?> registryKey;

    public Identifier(String namespace, String id) {
        setUp(namespace, id);
    }

    public Identifier(String compressed) {
        if (!compressed.contains(":")) {
            throw new IllegalStateException("Illegal compressed identifier. It should be like: \"namespace:id\"; But we found: " + compressed);
        }

        String[] parts = compressed.split(":");

        if (parts.length < 2) {
            throw new IllegalStateException("Illegal compressed identifier. It is missing a part. It should be like: \"namespace:id\"; But we found: " + compressed);
        }

        setUp(parts[0], parts[1]);
    }

    private void setUp(String namespace, String id) {
        this.namespace = namespace;
        this.id = id;

        if (!isValidNamespace(namespace)) {
            throw new IllegalStateException("Illegal namespace character in: " + this);
        }

        if (!isValidNId(namespace)) {
            throw new IllegalStateException("Illegal id character in: " + this);
        }
    }

    public static boolean isValidNamespace(String string) {
        return isValidString(string, SharedConstants.VALID_NAMESPACE_CHARS);
    }

    public static boolean isValidNId(String string) {
        return isValidString(string, SharedConstants.VALID_PATH_CHARS);
    }

    private static boolean isValidString(String string, String validChars) {
        for (int i = 0; i < string.length(); ++i) {
            if (validChars.indexOf(string.charAt(i)) == -1) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return this.namespace + ":" + this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Identifier that = (Identifier) obj;
        return namespace.equals(that.namespace) && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, id);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getId() {
        return id;
    }

    public void setRegistryKey(RegistryKey<?> registryKey) {
        this.registryKey = registryKey;
    }

    public RegistryKey<?> getRegistryKey() {
        return registryKey;
    }
}
