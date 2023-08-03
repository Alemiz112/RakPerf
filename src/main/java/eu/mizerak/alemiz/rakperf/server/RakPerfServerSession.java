package eu.mizerak.alemiz.rakperf.server;

import eu.mizerak.alemiz.rakperf.channel.RakPerfConnection;
import eu.mizerak.alemiz.rakperf.channel.codec.CompressionCodecs;
import eu.mizerak.alemiz.rakperf.channel.codec.EncryptionCodecs;
import eu.mizerak.alemiz.rakperf.channel.data.ConnectionSettings;
import eu.mizerak.alemiz.rakperf.channel.packet.ConnectionInitPacket;
import eu.mizerak.alemiz.rakperf.channel.packet.PayloadPacket;
import eu.mizerak.alemiz.rakperf.channel.packet.ServerHandshakePacket;
import eu.mizerak.alemiz.rakperf.utils.EncryptionHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import it.unimi.dsi.fastutil.Pair;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RakPerfServerSession extends RakPerfConnection {

    private final Channel channel;
    private final RakPerfServer server;

    private ConnectionSettings settings;

    public RakPerfServerSession(Channel channel, RakPerfServer server) {
        this.channel = channel;
        this.server = server;
        channel.attr(ATTRIBUTE).set(this);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("RakPerf session has disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[{}] Exception caught in server session", ctx.channel().remoteAddress(), cause);
        this.channel.disconnect();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ConnectionInitPacket) {
            this.onConnectionInit((ConnectionInitPacket) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private void onConnectionInit(ConnectionInitPacket packet) throws Exception {
        this.settings = packet.getSettings();

        CompressionCodecs.init(this.channel, settings.getCompression());

        String encryptionKey = null;
        if (settings.getEncryptionKey() != null) {
            PublicKey clientPublic = settings.getEncryptionKey().getPublic();

            Pair<KeyPair, byte[]> pair = EncryptionHelper.createEncryptionPair();

            SecretKey serverKey = EncryptionUtils.getSecretKey(pair.left().getPrivate(), clientPublic, pair.right());
            encryptionKey = EncryptionHelper.createHandshakeJwt(pair.left(), pair.right()).serialize();

            this.channel.eventLoop().schedule(() -> EncryptionCodecs.init(this.channel, serverKey), 5, TimeUnit.MILLISECONDS);
        }
        this.channel.writeAndFlush(new ServerHandshakePacket(encryptionKey));

        log.info("New RakPerf connection from {}: compression={} encrypt={} bidirectional={}", this.channel.remoteAddress(), settings.getCompression(), (settings.getEncryptionKey() != null), settings.isBidirectional());
    }

    @Override
    protected void onPayloadPacket(PayloadPacket packet) {
        super.onPayloadPacket(packet);

        ByteBuf payload = packet.getPayload();
        try {
            packet.setReplyTimestamp(System.currentTimeMillis());
            if (this.settings.isBidirectional()) {
                packet.setPayload(PayloadPacket.generatePayload(payload.readableBytes()));
            } else {
                packet.setPayload(null);
                packet.setPayloadSize(payload.readableBytes());
            }
            this.channel.writeAndFlush(packet);
        } finally {
            payload.release();
        }
    }
}
