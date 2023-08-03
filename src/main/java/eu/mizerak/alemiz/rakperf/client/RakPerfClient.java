package eu.mizerak.alemiz.rakperf.client;

import com.nimbusds.jwt.SignedJWT;
import eu.mizerak.alemiz.rakperf.channel.RakPerfConnection;
import eu.mizerak.alemiz.rakperf.channel.codec.CompressionCodecs;
import eu.mizerak.alemiz.rakperf.channel.codec.EncryptionCodecs;
import eu.mizerak.alemiz.rakperf.channel.data.ConnectionSettings;
import eu.mizerak.alemiz.rakperf.channel.packet.ConnectionInitPacket;
import eu.mizerak.alemiz.rakperf.channel.packet.PayloadPacket;
import eu.mizerak.alemiz.rakperf.channel.packet.ServerHandshakePacket;
import eu.mizerak.alemiz.rakperf.utils.EncryptionHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RakPerfClient extends RakPerfConnection {

    private final Channel channel;
    private final ConnectionSettings settings;

    private final int payloadSize;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private ScheduledFuture<?> sendFuture;
    private ScheduledFuture<?> infoFuture;

    private volatile long lastInfoUpdate;
    private volatile long startTime;

    private final AtomicLong totalSpentTime = new AtomicLong(0);

    public RakPerfClient(Channel channel, ConnectionSettings settings, int payloadSize) {
        this.channel = channel;
        this.settings = settings;
        this.payloadSize = payloadSize;
        channel.attr(ATTRIBUTE).set(this);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel.eventLoop().schedule(this::onSetup, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        if (this.sendFuture != null) {
            this.sendFuture.cancel(true);
        }

        if (this.infoFuture != null) {
            this.infoFuture.cancel(true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception caught", cause);
        System.exit(1);
    }

    private void onSetup() {
        if (this.initialized.compareAndSet(true, false)) {
            return;
        }

        this.channel.writeAndFlush(new ConnectionInitPacket(settings));
        CompressionCodecs.init(this.channel, this.settings.getCompression());

        log.info("RakPerf client connected! Sending connection settings: compression={} encrypt={} bidirectional={}", settings.getCompression(), (settings.getEncryptionKey() != null), settings.isBidirectional());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServerHandshakePacket) {
            this.onServerHandshake((ServerHandshakePacket) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private void onServerHandshake(ServerHandshakePacket packet) throws Exception {
        log.info("Server sent handshake, starting performance testing ...");

        if (packet.getEncryptionKey() != null) {
            SignedJWT saltJwt = SignedJWT.parse(packet.getEncryptionKey());
            URI x5u = saltJwt.getHeader().getX509CertURL();
            ECPublicKey serverKey = EncryptionHelper.generateKey(x5u.toASCIIString());
            SecretKey key = EncryptionUtils.getSecretKey(
                    this.settings.getEncryptionKey().getPrivate(),
                    serverKey,
                    Base64.getDecoder().decode(saltJwt.getJWTClaimsSet().getStringClaim("salt"))
            );
            EncryptionCodecs.init(this.channel, key);
        }

        this.sendFuture = this.channel.eventLoop().scheduleAtFixedRate(this::sendPayload, 50, 50, TimeUnit.MILLISECONDS);

        this.startTime = System.currentTimeMillis();
        this.lastInfoUpdate = this.startTime;

        this.infoFuture = this.channel.eventLoop().scheduleAtFixedRate(() -> {
            long currTime = System.currentTimeMillis();
            long diff =  currTime - this.lastInfoUpdate;

            double diffSec = diff / 1000D;
            double bitRate = (this.getReceivedBytes() * 8 / 1024D / 1024D) / diffSec;

            double from = (this.lastInfoUpdate - startTime) / 1000D;
            double to = (currTime - startTime) / 1000D;

            DecimalFormat format = new DecimalFormat("0.000");

            long avgPing = this.totalSpentTime.get() / this.getReceivedPackets();

            log.info("{}-{} sec {} Packets {} Bytes {} Mbits/sec avgPing {}", format.format(from), format.format(to), this.getReceivedPackets(),
                    this.getReceivedBytes(), format.format(bitRate), avgPing);

            this.lastInfoUpdate = currTime;
            this.resetReceivedBytesCounter();
            this.totalSpentTime.set(0);
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void onPayloadPacket(PayloadPacket packet) {
        super.onPayloadPacket(packet);
        this.totalSpentTime.addAndGet(System.currentTimeMillis() - packet.getSendTimestamp());
    }

    private void sendPayload() {
        PayloadPacket packet = new PayloadPacket();
        packet.setSendTimestamp(System.currentTimeMillis());
        packet.setPayload(PayloadPacket.generatePayload(this.payloadSize));
        this.channel.writeAndFlush(packet).addListener(f -> {
            if (f.cause() != null) {
                f.cause().printStackTrace();
            }
        });
    }
}
