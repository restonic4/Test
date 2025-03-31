package com.chaotic_loom.game.networking.packets;

import com.chaotic_loom.game.core.Environment;
import com.chaotic_loom.game.networking.NetworkingManager;
import com.chaotic_loom.game.networking.components.Packet;
import com.chaotic_loom.game.networking.components.PacketBuffer;
import com.chaotic_loom.game.networking.components.User;
import io.netty.channel.ChannelHandlerContext;

public class LoginPacket extends Packet {
    public LoginPacket() {
        super(Environment.SERVER);
    }

    @Override
    public void handle(NetworkingManager networkingManager, ChannelHandlerContext ctx, PacketBuffer packetBuffer) {
        User user = packetBuffer.readUser();

        networkingManager.getLogger().warn("Login packet received by {}", user);
    }

    public void send(User user) {
        PacketBuffer packetBuffer = new PacketBuffer(getIdentifier());

        packetBuffer.writeUser(user);

        sendToChannel(packetBuffer);
    }
}
