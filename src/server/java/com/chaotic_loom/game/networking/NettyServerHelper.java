package com.chaotic_loom.game.networking;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NettyServerHelper {
    public static Channel init() {
        int port = 8080; // your chosen port
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        AtomicReference<Channel> channel = new AtomicReference<>();
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicReference<Exception> exception = new AtomicReference<>();

        int maxFrameLength = 2048; // maximum length of a packet
        int lengthFieldOffset = 0; // length field starts at index 0
        int lengthFieldLength = 4; // length is a 4-byte int
        int lengthAdjustment = 0;  // no adjustment, if the length field only contains the payload length
        int initialBytesToStrip = 4; // strip the length field from the output

        new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();

                                // IdleStateHandler: triggers events if no read within 60 sec or no write within 30 sec
                                //pipeline.addLast(new IdleStateHandler(60, 30, 0, TimeUnit.SECONDS));

                                pipeline.addLast(new LengthFieldBasedFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip));
                                pipeline.addLast(new ServerPacketChannelHandler());
                            }
                        });

                // Bind to localhost on the selected port
                ChannelFuture future = bootstrap.bind("localhost", port).sync();
                channel.set(future.channel());
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                failed.set(true);
                exception.set(e);
                throw new RuntimeException(e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }, "Networking").start();

        while (channel.get() == null && !failed.get());

        // We throw the exception found on the networking thread
        if (failed.get()) {
            throw new RuntimeException(exception.get());
        }

        return channel.get();
    }
}
