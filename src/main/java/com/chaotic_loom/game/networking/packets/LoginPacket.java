package com.chaotic_loom.game.networking.packets;

import com.chaotic_loom.game.networking.components.Packet;
import com.chaotic_loom.game.networking.components.PacketBuffer;
import io.netty.channel.ChannelHandlerContext;

public class LoginPacket extends Packet {
    @Override
    public void handle(ChannelHandlerContext ctx, PacketBuffer packetBuffer) {
        System.out.println("NO WAY. WE GOT A PACKET");
        System.out.println(packetBuffer.readString());
    }

    public void send() {
        PacketBuffer packetBuffer = new PacketBuffer(this);

        packetBuffer.writeString("hi hello packet");

        sendToChannel(packetBuffer);
    }
}
