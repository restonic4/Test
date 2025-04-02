package com.chaotic_loom.game.registries.components;

import com.chaotic_loom.game.registries.Registry;

import java.util.Objects;

public abstract class RegistryObject {
    private Identifier identifier;
    private boolean isPopulated = false;
    public short internalMappedRegistryID;

    public Identifier getIdentifier() {
        if (identifier == null) {
            throw new IllegalStateException("This RegistryObject is not populated yet");
        }

        return this.identifier;
    }

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    //Gets called when the game registers the object
    public void onPopulate() {
        this.internalMappedRegistryID = Registry.requestNewInternalMappedID(this);
        this.isPopulated = true;
    }

    public boolean isPopulated() {
        return this.isPopulated;
    }
    public short getInternalMappedRegistryID() {
        return internalMappedRegistryID;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RegistryObject that = (RegistryObject) o;
        return Objects.equals(identifier, that.identifier) && isPopulated == that.isPopulated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.identifier, this.isPopulated);
    }
}