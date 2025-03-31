package com.chaotic_loom.game.networking.packets;

import com.chaotic_loom.game.core.Environment;
import com.chaotic_loom.game.networking.NetworkingManager;
import com.chaotic_loom.game.networking.components.Packet;
import com.chaotic_loom.game.networking.components.PacketBuffer;
import io.netty.channel.ChannelHandlerContext;

public class LoginPacket extends Packet {
    public LoginPacket() {
        super(Environment.SERVER);
    }

    @Override
    public void handle(NetworkingManager networkingManager, ChannelHandlerContext ctx, PacketBuffer packetBuffer) {
        networkingManager.getLogger().warn("NO WAY. WE GOT A PACKET");
        networkingManager.getLogger().warn(packetBuffer.readString());
    }

    public void send() {
        PacketBuffer packetBuffer = new PacketBuffer(getIdentifier());

        packetBuffer.writeString("hi hello packet");

        sendToChannel(packetBuffer);
    }
}
