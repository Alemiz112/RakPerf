package eu.mizerak.alemiz.rakperf.channel.codec.handler;

import com.nimbusds.jwt.SignedJWT;
import eu.mizerak.alemiz.rakperf.channel.codec.SimplePacketHandler;
import eu.mizerak.alemiz.rakperf.channel.data.ConnectionSettings;
import eu.mizerak.alemiz.rakperf.channel.packet.ConnectionInitPacket;
import eu.mizerak.alemiz.rakperf.channel.packet.Packet;
import eu.mizerak.alemiz.rakperf.utils.EncryptionHelper;
import io.netty.buffer.ByteBuf;

import java.net.URI;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class ConnectionInitPacketHandler extends SimplePacketHandler<ConnectionInitPacket> {

    public ConnectionInitPacketHandler() {
        super(ConnectionInitPacket.PACKET_ID);
    }

    @Override
    protected void encode(ConnectionInitPacket packet, ByteBuf buffer) throws Exception {
        ConnectionSettings settings = packet.getSettings();

        buffer.writeByte(settings.getCompression().ordinal());
        buffer.writeBoolean(settings.isBidirectional());
        buffer.writeBoolean(settings.getEncryptionKey() != null);
        if (settings.getEncryptionKey() != null) {
            String handshakeJwt = EncryptionHelper.createHandshakeJwt(settings.getEncryptionKey(), settings.getEncryptionToken()).serialize();
            Packet.writeString(buffer, handshakeJwt);
        }
    }

    @Override
    protected ConnectionInitPacket decode(ByteBuf buffer) throws Exception {
        ConnectionSettings settings = new ConnectionSettings();
        settings.setCompression(ConnectionSettings.Compression.values()[buffer.readUnsignedByte()]);
        settings.setBidirectional(buffer.readBoolean());

        if (buffer.readBoolean()) {
            SignedJWT saltJwt = SignedJWT.parse(Packet.readString(buffer));
            URI x5u = saltJwt.getHeader().getX509CertURL();
            ECPublicKey serverKey = EncryptionHelper.generateKey(x5u.toASCIIString());

            KeyPair keyPair = new KeyPair(serverKey, null);

            byte[] token = Base64.getDecoder().decode(saltJwt.getJWTClaimsSet().getStringClaim("salt"));

            settings.setEncryptionToken(token);
            settings.setEncryptionKey(keyPair);
        }
        return new ConnectionInitPacket(settings);
    }
}
