package eu.mizerak.alemiz.rakperf.channel;

import eu.mizerak.alemiz.rakperf.channel.packet.PayloadPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

import java.util.concurrent.atomic.AtomicLong;

public abstract class RakPerfConnection extends ChannelInboundHandlerAdapter {

    public static final AttributeKey<RakPerfConnection> ATTRIBUTE = AttributeKey.newInstance("rakperf_connection");

    private final AtomicLong receivedPacketsCounter = new AtomicLong();
    private final AtomicLong receivedBytesCounter = new AtomicLong();
    private final AtomicLong receivedBytesTotalCounter = new AtomicLong();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PayloadPacket) {
            this.onPayloadPacket((PayloadPacket) msg);
        }
    }

    protected void onPayloadPacket(PayloadPacket packet) {
        this.receivedPacketsCounter.incrementAndGet();
        this.receivedBytesCounter.addAndGet(packet.getPayloadSize());
        this.receivedBytesTotalCounter.addAndGet(packet.getPayloadSize());
    }

    public void resetReceivedBytesCounter() {
        this.receivedPacketsCounter.set(0);
        this.receivedBytesCounter.set(0);
    }

    public long getReceivedPackets() {
        return this.receivedPacketsCounter.get();
    }

    public long getReceivedBytes() {
        return this.receivedBytesCounter.get();
    }

    public long getReceivedBytesTotal() {
        return this.receivedBytesTotalCounter.get();
    }
}
