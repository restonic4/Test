package com.chaotic_loom.game.networking;

import io.netty.channel.Channel;

public class NetworkingManager {
    private Channel channel;

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return this.channel;
    }
}
