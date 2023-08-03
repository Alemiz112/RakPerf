package eu.mizerak.alemiz.rakperf.channel.codec.handler;

import eu.mizerak.alemiz.rakperf.channel.codec.SimplePacketHandler;
import eu.mizerak.alemiz.rakperf.channel.packet.Packet;
import eu.mizerak.alemiz.rakperf.channel.packet.PayloadPacket;
import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.common.util.VarInts;

public class PayloadPacketHandler extends SimplePacketHandler<PayloadPacket> {

    public PayloadPacketHandler() {
        super(PayloadPacket.PACKET_ID);
    }

    @Override
    protected void encode(PayloadPacket packet, ByteBuf buffer) {
        buffer.writeLong(packet.getSendTimestamp());
        buffer.writeLong(packet.getReplyTimestamp());
        VarInts.writeUnsignedInt(buffer, packet.getPayloadSize());
        buffer.writeBoolean(packet.isHasPayload());
        if (packet.isHasPayload()) {
            Packet.writeVarIntBuf(buffer, packet.getPayload());
        }
    }

    @Override
    protected PayloadPacket decode(ByteBuf buffer) throws Exception {
        PayloadPacket packet = new PayloadPacket();
        packet.setSendTimestamp(buffer.readLong());
        packet.setReplyTimestamp(buffer.readLong());
        packet.setPayloadSize(VarInts.readUnsignedInt(buffer));
        packet.setHasPayload(buffer.readBoolean());
        if (packet.isHasPayload()) {
            packet.setPayload(Packet.readVarIntBuf(buffer));
        }
        return packet;
    }
}
