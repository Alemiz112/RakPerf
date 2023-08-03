package eu.mizerak.alemiz.rakperf.channel.packet;

import eu.mizerak.alemiz.rakperf.channel.data.ConnectionSettings;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectionInitPacket implements Packet {
    public static final byte PACKET_ID = (byte) 0xa1;

    private ConnectionSettings settings;
}
