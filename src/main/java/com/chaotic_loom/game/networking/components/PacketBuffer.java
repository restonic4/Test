package com.chaotic_loom.game.networking.components;

import com.chaotic_loom.game.registries.components.Identifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class PacketBuffer {
    private ByteBuf buffer;

    public PacketBuffer(Identifier identifier) {
        this.buffer = Unpooled.buffer();
        this.writeIdentifier(identifier);
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

    public void writeUUID(UUID uuid) {
        if (uuid == null) {
            this.buffer.writeBoolean(false); // Mark as not present
        } else {
            this.buffer.writeBoolean(true); // Mark as present
            this.buffer.writeLong(uuid.getMostSignificantBits());
            this.buffer.writeLong(uuid.getLeastSignificantBits());
        }
    }

    public UUID readUUID() {
        boolean present = this.buffer.readBoolean();
        if (!present) {
            return null; // Was marked as not present
        } else {
            long msb = this.buffer.readLong();
            long lsb = this.buffer.readLong();
            return new UUID(msb, lsb);
        }
    }

    public void writeUser(User user) {
        this.writeUUID(user.uuid());
        this.writeString(user.username());
    }

    public User readUser() {
        return new User(this.readUUID(), this.readString());
    }

    // Other

    public ByteBuf getFinalBuffer(Channel channel) {
        int dataLength = this.buffer.readableBytes();
        ByteBuf finalBuffer = channel.alloc().buffer(4 + dataLength);

        finalBuffer.writeInt(dataLength);
        finalBuffer.writeBytes(this.buffer);

        return finalBuffer;
    }

    @Override
    public String toString() {
        return "PacketBuffer{" +
                "buffer=" + buffer +
                '}';
    }
}
