package com.chaotic_loom.game.rendering;

import com.chaotic_loom.game.rendering.texture.Texture;
import com.chaotic_loom.game.rendering.texture.TextureAtlasInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class TextureManager {
    private static final Logger LOGGER = LogManager.getLogger("TextureManager");

    // Configuration
    private final int maxAtlasWidth = 2048;
    private final int maxAtlasHeight = 2048;
    private final int padding = 0;

    // State
    private final List<Texture> atlases = new ArrayList<>();
    private final Map<String, TextureAtlasInfo> textureInfoMap = new HashMap<>();
    private boolean baked = false;

    /**
     * Finds textures, packs them into atlases, uploads to GPU, and creates mapping info.
     * Should be called once during client initialization.
     * @param resourceScanPackages Packages to scan for texture resources.
     * Uses Reflections library for scanning JAR/classpath.
     */
    public synchronized void bakeAtlases(String... resourceScanPackages) {
        if (baked) {
            LOGGER.warn("Texture atlases already baked.");
            return;
        }

        LOGGER.info("Starting texture atlas baking process...");
        long startTime = System.nanoTime();


        // 1. Discover Texture Resources
        List<ImageToPack> imagesToPack = findTextureResources(resourceScanPackages);
        if (imagesToPack.isEmpty()) {
            LOGGER.warn("No textures found to bake into atlases.");
            baked = true;
            return;
        }

        LOGGER.info("Found {} texture resources to pack.", imagesToPack.size());


        // 2. Pack Textures into Atlas Bins (Using a simple packer for example)
        List<AtlasBin> bins = packTexturesMaxRects(imagesToPack);

        LOGGER.info("Packed textures into {} atlas bins.", bins.size());


        // 3. Generate OpenGL Textures and UV Map
        generateAtlasTextures(bins);
        LOGGER.info("Generated {} OpenGL atlas textures.", atlases.size());

        baked = true;
        long endTime = System.nanoTime();

        LOGGER.info("Texture atlas baking completed in {} ms.", (endTime - startTime) / 1_000_000);
    }

    /** Finds image resources using Reflections. */
    private List<ImageToPack> findTextureResources(String... packagesToScan) {
        List<ImageToPack> images = new ArrayList<>();
        Reflections reflections = new Reflections(packagesToScan, Scanners.Resources);

        Set<String> resourcePaths = reflections.getResources(Pattern.compile(".*\\.(png|jpg|jpeg|bmp|tga)$", Pattern.CASE_INSENSITIVE));

        for (String path : resourcePaths) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // Need to get resource as stream/URL to pass to STB info loader
                URL resourceUrl = getClass().getClassLoader().getResource(path);
                if (resourceUrl == null) {
                    LOGGER.warn("Could not find URL for resource: {}", path);
                    continue;
                }

                String canonicalPath = path.startsWith("/") ? path : "/" + path;
                images.add(new ImageToPack(canonicalPath, 0, 0)); // Dimensions loaded later

            } catch (Exception e) {
                LOGGER.error("Failed to process resource info: {}", path, e);
            }
        }
        return images;
    }

    /**
     * Packs a list of images into atlas bins using a MaxRects algorithm.
     * This method loads image dimensions, sorts images by descending area,
     * and attempts to place them into bins.
     * @param images The list of images to pack.
     * @return A list of AtlasBin objects containing the packed images.
     */
    private List<AtlasBin> packTexturesMaxRects(List<ImageToPack> images) {
        // Load image dimensions first
        LOGGER.debug("Loading image dimensions...");

        for (ImageToPack img : images) {
            try (MemoryStack stack = MemoryStack.stackPush();
                 InputStream is = getClass().getResourceAsStream(img.resourcePath)) {
                if (is == null) throw new IOException("Resource not found: " + img.resourcePath);
                byte[] bytes = is.readAllBytes();
                ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
                buffer.put(bytes).flip();

                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                if (!stbi_info_from_memory(buffer, w, h, comp)) {
                    throw new IOException("Failed to get image info: " + stbi_failure_reason());
                }
                img.width = w.get();
                img.height = h.get();
                MemoryUtil.memFree(buffer);
            } catch (Exception e) {
                LOGGER.error("Failed to load dimensions for {}: {}", img.resourcePath, e.getMessage());
                img.width = 0;
                img.height = 0;
            }
        }

        images.removeIf(img -> img.width <= 0 || img.height <= 0);

        // Sort by descending area (considering padding)
        images.sort((a, b) -> {
            int areaA = (a.width + padding) * (a.height + padding);
            int areaB = (b.width + padding) * (b.height + padding);
            return Integer.compare(areaB, areaA);
        });

        LOGGER.debug("Dimensions loaded. Starting MaxRects packing...");

        List<AtlasBin> bins = new ArrayList<>();

        for (ImageToPack img : images) {
            boolean placed = false;
            for (AtlasBin bin : bins) {
                if (bin.tryPlaceImage(img, padding)) {
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                // Create a new bin and try to place the image
                AtlasBin newBin = new AtlasBin(maxAtlasWidth, maxAtlasHeight);
                if (newBin.tryPlaceImage(img, padding)) {
                    bins.add(newBin);
                } else {
                    LOGGER.error("Texture {} ({}x{} with padding) is too large to fit in atlas ({}x{})",
                            img.resourcePath, img.width + padding, img.height + padding,
                            maxAtlasWidth, maxAtlasHeight);
                }
            }
        }

        return bins;
    }

    /** Loads image data, creates OpenGL textures for bins, stores UV info. */
    private void generateAtlasTextures(List<AtlasBin> bins) {
        stbi_set_flip_vertically_on_load(true); // Match OpenGL coord system

        for (int i = 0; i < bins.size(); i++) {
            AtlasBin bin = bins.get(i);
            if (bin.images.isEmpty()) continue;

            // Determine actual size used by this bin (optional, could use max size)
            int binWidth = maxAtlasWidth; // Or calculate tighter bounds
            int binHeight = maxAtlasHeight; // Or calculate tighter bounds

            ByteBuffer atlasBuffer = null;
            Texture atlasTexture = null;
            try {
                atlasBuffer = MemoryUtil.memAlloc(binWidth * binHeight * 4); // RGBA

                for (int p = 0; p < binWidth * binHeight; p++) {
                    atlasBuffer.putInt(0x00000000); // Transparent black ABGR? Needs checking
                }
                atlasBuffer.flip();

                // Load each image and blit it onto the atlas buffer
                for (ImageToPack img : bin.images) {
                    ByteBuffer imagePixelData = null;
                    try(MemoryStack stack = MemoryStack.stackPush();
                        InputStream is = getClass().getResourceAsStream(img.resourcePath)) {
                        if (is == null) throw new IOException("Resource not found: " + img.resourcePath);
                        byte[] bytes = is.readAllBytes();
                        ByteBuffer fileBuffer = MemoryUtil.memAlloc(bytes.length);
                        fileBuffer.put(bytes).flip();

                        IntBuffer w = stack.mallocInt(1);
                        IntBuffer h = stack.mallocInt(1);
                        IntBuffer comp = stack.mallocInt(1);

                        imagePixelData = stbi_load_from_memory(fileBuffer, w, h, comp, 4); // Force RGBA
                        if (imagePixelData == null) {
                            throw new IOException("Failed to load image data: " + stbi_failure_reason());
                        }

                        // Blit image data into atlas buffer
                        // Use STBImageWrite functions for robust blitting? Or manual copy:
                        blitPixels(imagePixelData, w.get(), h.get(), atlasBuffer, binWidth, img.atlasX, img.atlasY);

                        MemoryUtil.memFree(fileBuffer);

                    } finally {
                        if (imagePixelData != null) {
                            stbi_image_free(imagePixelData);
                        }
                    }
                }

                // --- Create OpenGL Texture ---
                atlasTexture = new Texture(); // Create empty texture object
                atlasTexture.setWidth(maxAtlasWidth);
                atlasTexture.setHeight(maxAtlasHeight);

                glBindTexture(GL_TEXTURE_2D, atlasTexture.getTextureId());
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); // Avoid bleeding from opposite edge
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, binWidth, binHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
                glGenerateMipmap(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, 0); // Unbind

                atlases.add(atlasTexture);
                LOGGER.info("Created Atlas Texture ID: {}", atlasTexture.getTextureId());

                // --- Now store TextureAtlasInfo using the created texture ---
                for (ImageToPack img : bin.images) {
                    if (img.width <= 0 || img.height <= 0) continue; // Skip failed loads

                    float u0 = (float) (img.atlasX) / binWidth;
                    float v0 = (float) (img.atlasY) / binHeight;
                    float u1 = (float) (img.atlasX + img.width) / binWidth;
                    float v1 = (float) (img.atlasY + img.height) / binHeight;

                    TextureAtlasInfo info = new TextureAtlasInfo(atlasTexture, u0, v0, u1, v1);
                    textureInfoMap.put(img.resourcePath, info); // Store by canonical path

                    LOGGER.debug("Mapped {} to Atlas {} UVs: ({},{})-({},{})", img.resourcePath, i, u0, v0, u1, v1);
                }

                // --- Debug: Save atlas to file ---
                // TODO: args
                saveAtlasToFile(atlasBuffer, binWidth, binHeight, "atlas_" + i + ".png");
            } catch (Exception e) {
                LOGGER.error("Failed to generate atlas texture for bin {}: {}", i, e.getMessage(), e);

                // Cleanup partially created texture if necessary
                if(atlasTexture != null) atlasTexture.cleanup();
            } finally {
                if (atlasBuffer != null) {
                    MemoryUtil.memFree(atlasBuffer);
                }
            }
        }

        stbi_set_flip_vertically_on_load(false); // Reset STB flip state
    }

    // Simple pixel blitting - assumes RGBA, 4 bytes per pixel
    private void blitPixels(ByteBuffer srcBuffer, int srcWidth, int srcHeight, ByteBuffer dstBuffer, int dstWidth, int dstX, int dstY) {
        int srcStride = srcWidth * 4;
        int dstStride = dstWidth * 4;

        for (int y = 0; y < srcHeight; y++) {
            // Set position in destination buffer
            dstBuffer.position((dstY + y) * dstStride + dstX * 4);
            // Set position and limit in source buffer for the current row
            srcBuffer.position(y * srcStride);
            srcBuffer.limit((y + 1) * srcStride);
            // Copy row (limit source buffer prevents reading too much)
            dstBuffer.put(srcBuffer);
        }

        // Reset positions/limits if buffers are reused (important!)
        srcBuffer.clear();
        dstBuffer.clear();
    }

    /** Debug method to save buffer as PNG **/
    private void saveAtlasToFile(ByteBuffer buffer, int width, int height, String filename) {
        try {
            Path path = Paths.get(filename);
            LOGGER.debug("Saving atlas debug image to: {}", path.toAbsolutePath());
            // STBImageWrite expects data starting from position 0
            buffer.rewind();
            if (!STBImageWrite.stbi_write_png(path.toString(), width, height, 4, buffer, width * 4)) {
                LOGGER.error("Failed to write debug atlas image!");
            }
            buffer.rewind(); // Rewind again after write
        } catch (Exception e) {
            LOGGER.error("Error saving debug atlas image", e);
        }
    }


    /**
     * Gets the atlas information (atlas texture + UVs) for a given texture resource path.
     * Assumes bakeAtlases() has been called.
     * @param resourcePath The original texture path (e.g., "/assets/textures/blocks/you_just_lost_the_game.png").
     * @return TextureAtlasInfo containing the atlas and UVs, or null if not found.
     */
    public TextureAtlasInfo getTextureInfo(String resourcePath) {
        if (!baked) {
            LOGGER.warn("Attempted to get texture info before atlases were baked.");
            return null;
        }

        String canonicalPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        TextureAtlasInfo info = textureInfoMap.get(canonicalPath);

        if (info == null) {
            LOGGER.warn("Texture info not found for: {}", canonicalPath);
        }

        return info;
    }

    /** Get specific atlas texture by index */
    public Texture getAtlasTexture(int index) {
        if (index >= 0 && index < atlases.size()) {
            return atlases.get(index);
        }

        return null;
    }

    /** Cleanup all loaded atlas textures */
    public void cleanup() {
        LOGGER.info("Cleaning up TextureManager...");

        for (Texture atlas : atlases) {
            atlas.cleanup();
        }

        atlases.clear();
        textureInfoMap.clear();
        baked = false;

        LOGGER.info("TextureManager cleanup complete.");
    }


    // --- Helper Classes for Packing ---
    private static class ImageToPack {
        final String resourcePath;
        int width;
        int height;
        int atlasX; // Position within the generated atlas
        int atlasY;

        ImageToPack(String resourcePath, int width, int height) {
            this.resourcePath = resourcePath;
            this.width = width;
            this.height = height;
        }
    }

    private static class AtlasBin {
        final int width;
        final int height;
        final List<ImageToPack> images = new ArrayList<>();
        final List<FreeRectangle> freeRectangles = new ArrayList<>();

        AtlasBin(int width, int height) {
            this.width = width;
            this.height = height;

            // Initialize with the entire atlas area as a free rectangle
            this.freeRectangles.add(new FreeRectangle(0, 0, width, height));
        }

        boolean tryPlaceImage(ImageToPack img, int padding) {
            int requiredWidth = img.width + padding;
            int requiredHeight = img.height + padding;

            FreeRectangle bestRect = getFreeRectangle(requiredWidth, requiredHeight);

            if (bestRect == null) {
                return false; // No space in this bin
            }

            // Place the image
            img.atlasX = bestRect.x;
            img.atlasY = bestRect.y;
            images.add(img);

            // Remove the used free rectangle
            freeRectangles.remove(bestRect);

            // Split the remaining space into new free rectangles
            int remainingWidth = bestRect.width - requiredWidth;
            int remainingHeight = bestRect.height - requiredHeight;

            // Split to the right of the placed image
            if (remainingWidth > 0 && requiredHeight > 0) {
                freeRectangles.add(new FreeRectangle(
                        bestRect.x + requiredWidth,
                        bestRect.y,
                        remainingWidth,
                        requiredHeight
                ));
            }

            // Split below the placed image
            if (remainingHeight > 0 && bestRect.width > 0) {
                freeRectangles.add(new FreeRectangle(
                        bestRect.x,
                        bestRect.y + requiredHeight,
                        bestRect.width,
                        remainingHeight
                ));
            }

            return true;
        }

        @Nullable
        private FreeRectangle getFreeRectangle(int requiredWidth, int requiredHeight) {
            FreeRectangle bestRect = null;
            int bestScore = Integer.MAX_VALUE;

            // Find the best free rectangle using Best Area Fit heuristic
            for (FreeRectangle rect : freeRectangles) {
                if (rect.width >= requiredWidth && rect.height >= requiredHeight) {
                    int areaFit = (rect.width * rect.height) - (requiredWidth * requiredHeight);
                    if (areaFit < bestScore) {
                        bestScore = areaFit;
                        bestRect = rect;
                    }
                }
            }
            return bestRect;
        }
    }

    private record FreeRectangle(int x, int y, int width, int height) {}
}
