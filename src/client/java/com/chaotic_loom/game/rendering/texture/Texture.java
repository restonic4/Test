package com.chaotic_loom.game.rendering.texture;

import com.chaotic_loom.game.util.Experimental;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.*;

public class Texture {
    private final int textureId;
    private int width;
    private int height;

    // Empty texture with no data
    public Texture() {
        textureId = glGenTextures();
    }

    // Creates a texture and loads data
    @Experimental
    public Texture(String resourcePath) throws Exception {
        this(); // Generate ID
        loadFromClasspath(resourcePath);
    }

    @Experimental
    public void loadFromClasspath(String resourcePath) throws Exception {
        ByteBuffer imageBuffer;
        Path path = null;

        // Try loading from classpath resources
        try (InputStream source = Texture.class.getResourceAsStream(resourcePath);
             ReadableByteChannel rbc = Channels.newChannel(source))
        {
            if (source == null) {
                throw new IOException("Classpath resource not found: " + resourcePath);
            }

            // Allocate buffer (consider using MemoryUtil.memAlloc for large files if needed)
            imageBuffer = ByteBuffer.allocateDirect(1024 * 1024); // 1MB buffer, might need resizing logic
            while (rbc.read(imageBuffer) != -1) {
                if (imageBuffer.remaining() == 0) {
                    // Resize buffer if needed (complex) - simpler to assume fits or use file path method
                    throw new IOException("Buffer too small for resource: " + resourcePath);
                }
            }

            imageBuffer.flip();
        } catch (IOException e) {
            System.err.println("Failed to load texture from classpath: " + resourcePath);
            throw e;
        }

        uploadTextureData(imageBuffer);
        stbi_image_free(imageBuffer);
    }

    @Experimental
    public void loadFromFile(Path filePath) throws Exception {
        ByteBuffer imageBuffer;
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load image using STB
            String filePathStr = filePath.toAbsolutePath().toString();
            imageBuffer = stbi_load(filePathStr, w, h, channels, 4); // Request 4 channels (RGBA)
            if (imageBuffer == null) {
                throw new Exception("Could not load image file: " + filePathStr + " - " + stbi_failure_reason());
            }

            this.width = w.get();
            this.height = h.get();
        }

        uploadTextureData(imageBuffer);

        // Free the image buffer allocated by stbi_load
        stbi_image_free(imageBuffer);
    }

    @Experimental
    private void uploadTextureData(ByteBuffer imageBuffer) {
        // Bind the texture
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Tell OpenGL how to unpack the RGBA bytes. Each component is 1 byte size
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1); // Disable byte-alignment restrictions

        // Set texture filtering parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR); // Choose filter modes
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST); // Magnification usually doesn't use mipmaps

        // Set texture wrapping parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); // Repeat texture on S axis
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT); // Repeat texture on T axis

        // Upload the texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);

        // Generate Mipmaps
        glGenerateMipmap(GL_TEXTURE_2D);

        // Unbind texture (optional, good practice)
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Binds the texture to the specified texture unit.
     * @param unit The texture unit index (0, 1, 2, ...).
     */
    public void bind(int unit) {
        if (unit < 0 || unit >= 32) { // Check against reasonable limit
            throw new IllegalArgumentException("Texture unit index out of range: " + unit);
        }

        glActiveTexture(GL_TEXTURE0 + unit); // Activate the texture unit first
        glBindTexture(GL_TEXTURE_2D, textureId); // Bind this texture to the active unit
    }

    /** Unbinds whatever texture is on the target unit. */
    public void unbind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /** Deletes the OpenGL texture object. */
    public void cleanup() {
        glDeleteTextures(textureId);
    }


    // --- SETTERS ---
    public void setWidth(int width) {
        this.width = width;
    }
    public void setHeight(int height) {
        this.height = height;
    }


    // --- GETTERS ---
    public int getTextureId() {
        return textureId;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
}