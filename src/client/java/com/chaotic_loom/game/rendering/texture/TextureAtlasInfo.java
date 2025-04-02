package com.chaotic_loom.game.rendering.texture;

import java.util.Objects;

/**
 * @param u0 min U
 * @param v0 min V
 * @param u1 max U
 * @param v1 max V
 */
public record TextureAtlasInfo(Texture atlasTexture, float u0, float v0, float u1, float v1) {
    public float getWidthUV() {
        return u1 - u0;
    }

    public float getHeightUV() {
        return v1 - v0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextureAtlasInfo that = (TextureAtlasInfo) o;

        return Objects.equals(atlasTexture, that.atlasTexture) &&
                Float.compare(that.u0, u0) == 0 &&
                Float.compare(that.v0, v0) == 0 &&
                Float.compare(that.u1, u1) == 0 &&
                Float.compare(that.v1, v1) == 0;
    }
}