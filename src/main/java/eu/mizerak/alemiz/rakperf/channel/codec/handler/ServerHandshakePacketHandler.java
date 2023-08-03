package eu.mizerak.alemiz.rakperf.channel.codec.handler;

import eu.mizerak.alemiz.rakperf.channel.codec.SimplePacketHandler;
import eu.mizerak.alemiz.rakperf.channel.packet.Packet;
import eu.mizerak.alemiz.rakperf.channel.packet.ServerHandshakePacket;
import io.netty.buffer.ByteBuf;

public class ServerHandshakePacketHandler extends SimplePacketHandler<ServerHandshakePacket> {

    public ServerHandshakePacketHandler() {
        super(ServerHandshakePacket.PACKET_ID);
    }

    @Override
    protected void encode(ServerHandshakePacket packet, ByteBuf buffer) {
        buffer.writeBoolean(packet.getEncryptionKey() != null);
        if (packet.getEncryptionKey() != null) {
            Packet.writeString(buffer, packet.getEncryptionKey());
        }
    }

    @Override
    protected ServerHandshakePacket decode(ByteBuf buffer) throws Exception {
        String key = null;
        if (buffer.readBoolean()) {
            key = Packet.readString(buffer);
        }
        return new ServerHandshakePacket(key);
    }
}
