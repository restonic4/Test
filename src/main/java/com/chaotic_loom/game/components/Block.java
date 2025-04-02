package com.chaotic_loom.game.components;

import com.chaotic_loom.game.registries.components.RegistryObject;

public class Block extends RegistryObject {
    private final Settings settings;

    public Block(Settings settings) {
        this.settings = settings;
    }

    public static class Settings {
        private final boolean isTransparent;
        private final boolean hasCollider;

        private Settings(Builder builder) {
            this.isTransparent = builder.isTransparent;
            this.hasCollider = builder.hasCollider;
        }

        public static class Builder {
            private boolean isTransparent = false;
            private boolean hasCollider = true;

            public Builder setTransparent(boolean isTransparent) {
                this.isTransparent = isTransparent;
                return this;
            }

            public Builder setCollider(boolean hasCollider) {
                this.hasCollider = hasCollider;
                return this;
            }

            public Settings build() {
                return new Settings(this);
            }
        }
    }
}


