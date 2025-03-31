package com.chaotic_loom.game.core;

import com.chaotic_loom.game.networking.ClientPacketChannelHandler;
import com.chaotic_loom.game.networking.NetworkingManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class TempServer {
    public static void joinServer(ClientEngine engine) {
        new Thread(() -> {
            String host = "localhost";
            int port = 8080;
            EventLoopGroup group = new NioEventLoopGroup();

            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();

                                pipeline.addLast(new IdleStateHandler(NetworkingManager.READER_IDLE_TIMEOUT_SECONDS, 0, 0, TimeUnit.SECONDS));
                                pipeline.addLast(new LengthFieldBasedFrameDecoder(NetworkingManager.MAX_FRAME_LENGTH, NetworkingManager.LENGTH_FIELD_OFFSET, NetworkingManager.LENGTH_FIELD_LENGTH, NetworkingManager.LENGTH_ADJUSTMENT, NetworkingManager.INITIAL_BYTES_TO_STRIP));
                                pipeline.addLast(new ClientPacketChannelHandler());
                            }
                        });

                // Connect to the server and wait until the connection is made.
                ChannelFuture future = bootstrap.connect(host, port).sync();
                System.out.println("Connected to server: " + future.channel().remoteAddress());
                engine.getNetworkingManager().setChannel(future.channel());
                // Keep the client running until the connection is closed.
                future.channel().closeFuture().sync();
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                group.shutdownGracefully();
            }
        }).start();
    }
}
