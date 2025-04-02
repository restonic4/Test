package com.chaotic_loom.game.registries.built_in;

import com.chaotic_loom.game.world.components.Block;
import com.chaotic_loom.game.core.Environment;
import com.chaotic_loom.game.core.util.SharedConstants;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.components.Identifier;
import com.chaotic_loom.game.registries.components.Registration;

@Registration(environment = Environment.COMMON)
public class Blocks {
    public static Block AIR;
    public static Block GLASS;
    public static Block DIRT;
    public static Block WOOD;
    public static Block STONE;
    public static Block LOG;

    public static void register() {
        AIR = Registry.register(
                RegistryKeys.BLOCK,
                new Identifier(SharedConstants.NAMESPACE, "air"),
                new Block(
                        new Block.Settings.Builder()
                                .setTransparent(true)
                                .setCollider(false)
                                .build()
                )
        );

        GLASS = Registry.register(
                RegistryKeys.BLOCK,
                new Identifier(SharedConstants.NAMESPACE, "glass"),
                new Block(
                        new Block.Settings.Builder()
                                .setTransparent(true)
                                .setCollider(true)
                                .setFaceProperties(
                                        new Block.FaceProperties.Builder()
                                                .setTextures("textures/glass.png")
                                                .build()
                                )
                                .build()
                )
        );

        DIRT = Registry.register(
                RegistryKeys.BLOCK,
                new Identifier(SharedConstants.NAMESPACE, "dirt"),
                new Block(
                        new Block.Settings.Builder()
                                .setTransparent(false)
                                .setCollider(true)
                                .setFaceProperties(
                                        new Block.FaceProperties.Builder()
                                                .setTextures("textures/dirt.png")
                                                .build()
                                )
                                .build()
                )
        );

        WOOD = Registry.register(
                RegistryKeys.BLOCK,
                new Identifier(SharedConstants.NAMESPACE, "wood"),
                new Block(
                        new Block.Settings.Builder()
                                .setTransparent(false)
                                .setCollider(true)
                                .setFaceProperties(
                                        new Block.FaceProperties.Builder()
                                                .setTextures("textures/wood.png")
                                                .build()
                                )
                                .build()
                )
        );

        STONE = Registry.register(
                RegistryKeys.BLOCK,
                new Identifier(SharedConstants.NAMESPACE, "stone"),
                new Block(
                        new Block.Settings.Builder()
                                .setTransparent(false)
                                .setCollider(true)
                                .setFaceProperties(
                                        new Block.FaceProperties.Builder()
                                                .setTextures("textures/stone.png")
                                                .build()
                                )
                                .build()
                )
        );

        LOG = Registry.register(
                RegistryKeys.BLOCK,
                new Identifier(SharedConstants.NAMESPACE, "log"),
                new Block(
                        new Block.Settings.Builder()
                                .setTransparent(false)
                                .setCollider(true)
                                .setFaceProperties(
                                        new Block.FaceProperties.Builder()
                                                .setTextures("textures/log.png")
                                                .build()
                                )
                                .allowAllDirections()
                                .build()
                )
        );
    }
}
