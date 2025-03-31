package com.chaotic_loom.game.networking;

import io.netty.channel.ChannelHandlerContext;

public class ServerPacketChannelHandler extends PacketChannelHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
    }
}