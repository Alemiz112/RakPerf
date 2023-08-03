package eu.mizerak.alemiz.rakperf.channel.packet;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServerHandshakePacket implements Packet {
    public static final byte PACKET_ID = (byte) 0xa2;

    private String encryptionKey;
}
