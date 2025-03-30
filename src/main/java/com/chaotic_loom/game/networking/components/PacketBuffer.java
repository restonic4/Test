package com.chaotic_loom.game.networking.components;

import com.chaotic_loom.game.registries.components.Identifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class PacketBuffer {
    private final ByteBuf buffer;

    public PacketBuffer(Packet packet) {
        this.buffer = Unpooled.buffer();
        writeIdentifier(packet.getIdentifier());
    }

    public PacketBuffer(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public void writeString(String s) {
        if (s == null) {
            this.buffer.writeInt(-1);
        } else {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

            this.buffer.writeInt(bytes.length);
            this.buffer.writeBytes(bytes);
        }
    }

    public String readString() {
        int length = this.buffer.readInt();
        if (length < 0) {
            return null;
        }

        byte[] bytes = new byte[length];
        this.buffer.readBytes(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public void writeIdentifier(Identifier identifier) {
        writeString(identifier.toString());
    }

    public Identifier readIdentifier() {
        String rawIdentifier = readString();
        return new Identifier(rawIdentifier);
    }
}
