package eu.mizerak.alemiz.rakperf.channel.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.ThreadLocalRandom;

@Data
@EqualsAndHashCode(callSuper = true)
public class PayloadPacket extends AbstractReferenceCounted implements Packet {
    private static ByteBuf[] payloads = new ByteBuf[8];
    static {
        for (int i = 0; i < payloads.length; i++) {
            byte[] payload = new byte[1024 * 1024];
            ThreadLocalRandom.current().nextBytes(payload);
            payloads[i] = Unpooled.wrappedBuffer(payload);
        }
    }

    public static final byte PACKET_ID = (byte) 0xa3;

    private long sendTimestamp;
    private long replyTimestamp;
    private int payloadSize;
    private boolean hasPayload;
    private ByteBuf payload;

    @Override
    protected void deallocate() {
        if (this.payload != null) {
            this.payload.release();
        }
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        if (this.payload != null) {
            this.payload.touch(hint);
        }
        return this;
    }

    public void setPayload(ByteBuf payload) {
        this.payload = payload;
        this.hasPayload = payload != null;
        if (payload != null) {
            this.payloadSize = payload.readableBytes();
        }
    }

    public static ByteBuf generatePayload(int size) {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer(size);

        ByteBuf sample = payloads[ThreadLocalRandom.current().nextInt(payloads.length)];
        buffer.writeBytes(sample.slice(0, Math.min(size, sample.readableBytes())));
        return buffer;
    }
}