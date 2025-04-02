package com.chaotic_loom.game.components;

import com.chaotic_loom.game.registries.components.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class Block extends RegistryObject {
    private final Settings settings;

    public Block(Settings settings) {
        this.settings = settings;
    }

    public static class Settings {
        private final boolean isTransparent;
        private final boolean hasCollider;
        private final FaceProperties faceProperties;

        private Settings(Builder builder) {
            this.isTransparent = builder.isTransparent;
            this.hasCollider = builder.hasCollider;
            this.faceProperties = builder.faceProperties;
        }

        public static class Builder {
            private boolean isTransparent = false;
            private boolean hasCollider = true;
            private FaceProperties faceProperties = new FaceProperties.Builder().build();

            public Builder setTransparent(boolean isTransparent) {
                this.isTransparent = isTransparent;
                return this;
            }

            public Builder setCollider(boolean hasCollider) {
                this.hasCollider = hasCollider;
                return this;
            }

            public Builder setFaceProperties(FaceProperties faceProperties) {
                this.faceProperties = faceProperties;
                return this;
            }

            public Settings build() {
                return new Settings(this);
            }
        }
    }

    public static class FaceProperties {
        private final Map<Face, String> textures;

        private FaceProperties(Builder builder) {
            this.textures = builder.textures;
        }

        public static class Builder {
            private final Map<Face, String> textures = new HashMap<>();

            public Builder setFaceTexture(Face face, String texturePath) {
                this.textures.put(face, texturePath);
                return this;
            }

            public Builder setTextures(String texturePath) {
                this.textures.put(Face.TOP, texturePath);
                this.textures.put(Face.BOTTOM, texturePath);
                this.textures.put(Face.RIGHT, texturePath);
                this.textures.put(Face.LEFT, texturePath);
                this.textures.put(Face.FRONT, texturePath);
                this.textures.put(Face.BACK, texturePath);
                return this;
            }

            public FaceProperties build() {
                return new FaceProperties(this);
            }
        }
    }

    public static enum Face {
        TOP, BOTTOM, RIGHT, LEFT, FRONT, BACK
    }
}


