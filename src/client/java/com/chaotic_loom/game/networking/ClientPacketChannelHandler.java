package com.chaotic_loom.game.networking;

import com.chaotic_loom.game.core.AbstractLauncher;
import com.chaotic_loom.game.core.ClientEngine;
import com.chaotic_loom.game.registries.built_in.Packets;
import io.netty.channel.ChannelHandlerContext;

public class ClientPacketChannelHandler extends PacketChannelHandler {
    // On the client, we send the LOGIN packet once the client joins a channel/server
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientEngine engine = (ClientEngine) AbstractLauncher.getEngine();
        ClientNetworkingContext context = engine.getClientNetworkingContext();

        Packets.LOGIN.send(context.getUser());
    }
}