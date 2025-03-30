package com.chaotic_loom.game.registries.components;

public abstract class RegistryObject {
    private Identifier identifier;
    private boolean isPopulated = false;

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
        this.isPopulated = true;
    }

    public boolean isPopulated() {
        return this.isPopulated;
    }
}