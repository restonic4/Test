package com.chaotic_loom.game.networking;

import com.chaotic_loom.game.core.AbstractEngine;
import com.chaotic_loom.game.core.AbstractLauncher;
import com.chaotic_loom.game.networking.components.Packet;
import com.chaotic_loom.game.networking.components.PacketBuffer;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.built_in.RegistryKeys;
import com.chaotic_loom.game.registries.components.Identifier;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkingManager {
    public static final int READER_IDLE_TIMEOUT_SECONDS = 60; // timeout seconds, time without packets being received

    public static final int MAX_FRAME_LENGTH = 2048; // maximum length of a packet
    public static final int LENGTH_FIELD_OFFSET = 0; // length field starts at index 0
    public static final int LENGTH_FIELD_LENGTH = 4; // length is a 4-byte int
    public static final int LENGTH_ADJUSTMENT = 0;  // no adjustment, if the length field only contains the payload length
    public static final int INITIAL_BYTES_TO_STRIP = 4; // strip the length field from the output

    private Channel channel;
    private Logger logger = LogManager.getLogger("Networking");

    public void send(Packet packet, PacketBuffer packetBuffer) {
        if (packet.getTarget() == AbstractLauncher.getEngine().getEnvironment()) {
            logger.warn("The packet {} is being sent to the same environment! That doesn't make any sense!", packet.getIdentifier());
            return;
        }

        ByteBuf finalByteBuffer = packetBuffer.getFinalBuffer(this.channel);
        this.channel.writeAndFlush(finalByteBuffer);
    }

    public void onPacketReceived(ChannelHandlerContext ctx, PacketBuffer packetBuffer) {
        Identifier packetId = packetBuffer.readIdentifier();
        Packet packet = Registry.getRegistryObject(RegistryKeys.PACKETS, packetId);

        if (packet == null) {
            logger.warn("The packet {} was not found!", packetId);
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

    public void cleanup() {
        logger.info("Cleaning networking manager");

        if (this.channel == null) {
            return;
        }

        this.channel.close();
        this.channel = null;
    }
}
