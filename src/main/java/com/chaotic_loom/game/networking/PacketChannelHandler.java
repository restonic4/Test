package com.chaotic_loom.game.networking;

import com.chaotic_loom.game.core.AbstractLauncher;
import com.chaotic_loom.game.networking.components.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.SocketAddress;

public abstract class PacketChannelHandler extends ChannelInboundHandlerAdapter {
    // Received packet
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        PacketBuffer packetBuffer = new PacketBuffer(in);

        AbstractLauncher.getEngine().getNetworkingManager().onPacketReceived(ctx, packetBuffer);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}