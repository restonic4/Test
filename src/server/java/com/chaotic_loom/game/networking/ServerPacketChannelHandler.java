package com.chaotic_loom.game.networking;

import com.chaotic_loom.game.core.AbstractLauncher;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.net.SocketAddress;

public class ServerPacketChannelHandler extends PacketChannelHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        Channel channel = ctx.channel();
        SocketAddress remoteAddress = channel.remoteAddress();

        AbstractLauncher.getEngine().getNetworkingManager().getLogger().warn("Client disconnected, {}", remoteAddress);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.READER_IDLE) {
                // Client hasn't sent anything for READER_IDLE_TIMEOUT_SECONDS

                Channel channel = ctx.channel();
                SocketAddress remoteAddress = channel.remoteAddress();

                AbstractLauncher.getEngine().getNetworkingManager().getLogger().warn("READER IDLE DETECTED for: {}. Closing connection.", remoteAddress);

                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}