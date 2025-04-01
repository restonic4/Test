package com.chaotic_loom.game.rendering;

import java.util.Objects;

public class TextureAtlasInfo {
    public final Texture atlasTexture;

    public final float u0; // min U
    public final float v0; // min V
    public final float u1; // max U
    public final float v1; // max V

    public TextureAtlasInfo(Texture atlasTexture, float u0, float v0, float u1, float v1) {
        this.atlasTexture = atlasTexture;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }

    public float getWidthUV() { return u1 - u0; }
    public float getHeightUV() { return v1 - v0; }

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

    @Override
    public int hashCode() {
        return Objects.hash(atlasTexture, u0, v0, u1, v1);
    }
}