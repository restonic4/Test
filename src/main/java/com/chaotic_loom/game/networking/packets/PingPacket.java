package com.chaotic_loom.game.networking.packets;

import com.chaotic_loom.game.core.Environment;
import com.chaotic_loom.game.core.Loggers;
import com.chaotic_loom.game.networking.NetworkingManager;
import com.chaotic_loom.game.networking.components.Packet;
import com.chaotic_loom.game.networking.components.PacketBuffer;
import com.chaotic_loom.game.networking.components.User;
import io.netty.channel.ChannelHandlerContext;

public class PingPacket extends Packet {
    public PingPacket() {
        super(Environment.SERVER);
    }

    @Override
    public void handle(NetworkingManager networkingManager, ChannelHandlerContext ctx, PacketBuffer packetBuffer) {
        User user = packetBuffer.readUser();
        Loggers.NETWORKING.debug("Ping packet received by {}", user);
    }

    public void send(User user) {
        PacketBuffer packetBuffer = new PacketBuffer(getIdentifier());

        packetBuffer.writeUser(user);

        sendToChannel(packetBuffer);
    }
}
