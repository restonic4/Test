package com.chaotic_loom.game.world.components;

import com.chaotic_loom.game.registries.components.RegistryObject;

import java.util.*;

public class Block extends RegistryObject {
    private final Settings settings;

    public Block(Settings settings) {
        this.settings = settings;
    }

    public BlockInstance createInstance() {
        return new BlockInstance(this, Direction.NORTH);
    }

    public Settings getSettings() {
        return settings;
    }

    @Override
    public String toString() {
        return "Block{" + getIdentifier() + "}";
    }

    public static class Settings {
        private final boolean isTransparent;
        private final boolean hasCollider;
        private final FaceProperties faceProperties;
        private final Set<Direction> allowedDirections;

        private Settings(Builder builder) {
            this.isTransparent = builder.isTransparent;
            this.hasCollider = builder.hasCollider;
            this.faceProperties = builder.faceProperties;
            this.allowedDirections = Collections.unmodifiableSet(EnumSet.copyOf(builder.allowedDirections));
        }

        public boolean isTransparent() {
            return isTransparent;
        }

        public boolean isHasCollider() {
            return hasCollider;
        }

        public FaceProperties getFaceProperties() {
            return faceProperties;
        }

        public Set<Direction> getAllowedDirections() {
            return allowedDirections;
        }

        public boolean isDirectionAllowed(Direction direction) {
            return direction != null && this.allowedDirections.contains(direction);
        }

        public static class Builder {
            private boolean isTransparent = false;
            private boolean hasCollider = true;
            private FaceProperties faceProperties = new FaceProperties.Builder().build();
            private Set<Direction> allowedDirections = EnumSet.of(Direction.NORTH);

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

            public Builder setAllowedDirections(Set<Direction> directions) {
                if (directions == null || directions.isEmpty()) {
                    // Default to only NORTH if set is null or empty
                    this.allowedDirections = EnumSet.of(Direction.NORTH);
                } else {
                    // Copy into an EnumSet for consistency and efficiency
                    this.allowedDirections = EnumSet.copyOf(directions);
                }
                return this;
            }

            public Builder setAllowedDirections(Direction first, Direction... rest) {
                this.allowedDirections = EnumSet.of(first, rest);
                return this;
            }

            public Builder noDirectional() {
                this.allowedDirections = EnumSet.of(Direction.NORTH);
                return this;
            }

            public Builder allowAllDirections() {
                this.allowedDirections = EnumSet.allOf(Direction.class);
                return this;
            }

            public Builder allowHorizontalDirections() {
                this.allowedDirections = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
                return this;
            }

            public Builder allowVerticalDirections() {
                this.allowedDirections = EnumSet.of(Direction.UP, Direction.DOWN);
                return this;
            }

            public Builder allowOnlyDirection(Direction direction) {
                if (direction == null) {
                    throw new IllegalArgumentException("Direction cannot be null");
                }
                this.allowedDirections = EnumSet.of(direction);
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

        public Map<Face, String> getTextures() {
            return textures;
        }

        public Face getFaceFromIndex(byte faceIndex) {
            for (Face face : Face.values()) {
                if (face.getFaceIndex() == faceIndex) {
                    return face;
                }
            }

            return null;
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

    /**
     * The indices are this way due to the class ChunkMesher on client side.
     */
    public static enum Face {
        TOP((byte) 2), BOTTOM((byte) 3), RIGHT((byte) 4), LEFT((byte) 5), FRONT((byte) 0), BACK((byte) 1);

        private final byte faceIndex;

        Face(byte faceIndex) {
            this.faceIndex = faceIndex;
        }

        public byte getFaceIndex() {
            return faceIndex;
        }
    }

    /**
     * Represents the 6 cardinal directions in 3D space.
     * Used for defining block orientation/facing.
     * Assuming standard coordinate system: +Y=Up, +Z=South, +X=East
     */
    public static enum Direction {
        DOWN(0, -1, 0),  // Y-
        UP(0, 1, 0),    // Y+
        NORTH(0, 0, -1), // Z-
        SOUTH(0, 0, 1),  // Z+
        WEST(-1, 0, 0),  // X-
        EAST(1, 0, 0);   // X+

        private final int dx, dy, dz;

        Direction(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        public int getDx() { return dx; }
        public int getDy() { return dy; }
        public int getDz() { return dz; }

        /** Gets the opposite direction. */
        public Direction getOpposite() {
            // Should not happen
            return switch (this) {
                case DOWN -> UP;
                case UP -> DOWN;
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case WEST -> EAST;
                case EAST -> WEST;
            };
        }

        /** Checks if the direction is along the X, Y, or Z axis. */
        public enum Axis { X, Y, Z }
        public Axis getAxis() {
            if (dx != 0) return Axis.X;
            if (dy != 0) return Axis.Y;
            if (dz != 0) return Axis.Z;
            throw new IllegalStateException("Direction has no axis component: " + this); // Should not happen
        }

        public boolean isHorizontal() {
            return dy == 0;
        }

        public boolean isVertical() {
            return dy != 0;
        }
    }
}


