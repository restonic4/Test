package com.chaotic_loom.game.networking.components;

import com.chaotic_loom.game.core.AbstractLauncher;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.components.RegistryObject;
import io.netty.channel.ChannelHandlerContext;

public abstract class Packet extends RegistryObject {
    public abstract void handle(ChannelHandlerContext ctx, PacketBuffer packetBuffer);

    public void sendToChannel(PacketBuffer packetBuffer) {
        AbstractLauncher.getEngine().getNetworkingManager().getChannel().writeAndFlush(packetBuffer);
    }
}