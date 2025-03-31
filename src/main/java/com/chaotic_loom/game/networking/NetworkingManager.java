package com.chaotic_loom.game.networking;

import com.chaotic_loom.game.core.AbstractEngine;
import com.chaotic_loom.game.core.AbstractLauncher;
import com.chaotic_loom.game.networking.components.Packet;
import com.chaotic_loom.game.networking.components.PacketBuffer;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.components.Identifier;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkingManager {
    private Channel channel;
    private Logger logger = LogManager.getLogger("Networking");

    public void send(Packet packet, PacketBuffer packetBuffer) {
        if (packet.getTarget() == AbstractLauncher.getEngine().getEnvironment()) {
            logger.warn("The packet {} is being sent to the same environment! That doesn't make any sense!", packet.getIdentifier());
            return;
        }

        this.channel.writeAndFlush(packetBuffer);
    }

    public void onPacketReceived(ChannelHandlerContext ctx, PacketBuffer packetBuffer) {
        Identifier packetId = packetBuffer.readIdentifier();
        Packet packet = Registry.getRegistryObject(packetId);

        if (packet == null) {
            logger.warn("The packet {} not found!", packetId);
            return;
        }

        if (packet.getTarget() != AbstractLauncher.getEngine().getEnvironment()) {
            logger.warn("The packet {} cant be sent to this environment! That doesn't make any sense!", packet.getIdentifier());
            return;
        }

        packet.handle(this, ctx, packetBuffer);
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return this.channel;
    }
    public Logger getLogger() {
        return logger;
    }
}
