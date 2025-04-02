package com.chaotic_loom.game.registries.built_in;

import com.chaotic_loom.game.components.Block;
import com.chaotic_loom.game.core.Environment;
import com.chaotic_loom.game.core.util.SharedConstants;
import com.chaotic_loom.game.networking.packets.LoginPacket;
import com.chaotic_loom.game.networking.packets.PingPacket;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.components.Identifier;
import com.chaotic_loom.game.registries.components.Registration;

@Registration(environment = Environment.COMMON)
public class Blocks {
    public static Block AIR;
    public static Block DIRT;
    public static Block WOOD;
    public static Block STONE;

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

        DIRT = Registry.register(
                RegistryKeys.BLOCK,
                new Identifier(SharedConstants.NAMESPACE, "dirt"),
                new Block(
                        new Block.Settings.Builder()
                                .setTransparent(false)
                                .setCollider(true)
                                .setFaceProperties(
                                        new Block.FaceProperties.Builder()
                                                .setTextures("dirt")
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
                                                .setTextures("wood")
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
                                                .setTextures("stone")
                                                .build()
                                )
                                .build()
                )
        );
    }
}
