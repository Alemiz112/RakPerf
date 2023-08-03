package eu.mizerak.alemiz.rakperf.channel.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.cloudburstmc.protocol.common.util.VarInts;

import java.nio.charset.StandardCharsets;

import static org.cloudburstmc.protocol.common.util.Preconditions.checkArgument;

public interface Packet {

    static void writeByteArray(ByteBuf buffer, byte[] bytes) {
        VarInts.writeUnsignedInt(buffer, bytes.length);
        buffer.writeBytes(bytes);
    }

    static byte[] readByteArray(ByteBuf buffer) {
        int length = VarInts.readUnsignedInt(buffer);
        checkArgument(buffer.isReadable(length), "Expected %s bytes but only %s left to read",
                length, buffer.readableBytes());
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return bytes;
    }

    static void writeVarIntBuf(ByteBuf buffer, ByteBuf bytes) {
        VarInts.writeUnsignedInt(buffer, bytes.readableBytes());
        buffer.writeBytes(bytes);
    }

    static ByteBuf readVarIntBuf(ByteBuf buffer) {
        int length = VarInts.readUnsignedInt(buffer);
        checkArgument(buffer.isReadable(), "Expected %s bytes but only %s left to read",
                length, buffer.readableBytes());
        return buffer.readRetainedSlice(length);
    }

    static String readString(ByteBuf buffer) {
        int length = VarInts.readUnsignedInt(buffer);
        checkArgument(buffer.isReadable(), "Expected %s bytes but only %s left to read",
                length, buffer.readableBytes());
        return (String) buffer.readCharSequence(length, StandardCharsets.UTF_8);
    }

    static void writeString(ByteBuf buffer, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Can not write null string!");
        }
        VarInts.writeUnsignedInt(buffer, ByteBufUtil.utf8Bytes(value));
        buffer.writeCharSequence(value, StandardCharsets.UTF_8);
    }
}
