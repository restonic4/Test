package com.chaotic_loom.game.networking.components;

import com.chaotic_loom.game.core.AbstractLauncher;
import com.chaotic_loom.game.core.Environment;
import com.chaotic_loom.game.networking.NetworkingManager;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.components.RegistryObject;
import io.netty.channel.ChannelHandlerContext;

public abstract class Packet extends RegistryObject {
    private final Environment target;

    public Packet(Environment target) {
        this.target = target;
    }

    public abstract void handle(NetworkingManager networkingManager, ChannelHandlerContext ctx, PacketBuffer packetBuffer);

    public void sendToChannel(PacketBuffer packetBuffer) {
        AbstractLauncher.getEngine().getNetworkingManager().send(this, packetBuffer);
    }

    public Environment getTarget() {
        return target;
    }
}