package com.chaotic_loom.game.networking;

import com.chaotic_loom.game.networking.components.Packet;
import com.chaotic_loom.game.networking.components.PacketBuffer;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.components.Identifier;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        PacketBuffer packetBuffer = new PacketBuffer(in);

        Identifier packetId = packetBuffer.readIdentifier();

        // Look up the corresponding singleton.
        Packet packet = Registry.getRegistryObject(packetId);
        if (packet != null) {
            packet.handle(ctx, packetBuffer);
        } else {
            System.err.println("Unknown packet identifier: " + packetId);
        }
    }
}