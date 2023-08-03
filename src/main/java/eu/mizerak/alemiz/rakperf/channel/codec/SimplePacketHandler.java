package eu.mizerak.alemiz.rakperf.channel.codec;

import eu.mizerak.alemiz.rakperf.channel.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;

public abstract class SimplePacketHandler<T extends Packet> extends ChannelDuplexHandler {

    private final byte packetId;
    private final TypeParameterMatcher matcher;

    public SimplePacketHandler(byte packetId) {
        this.packetId = packetId;
        this.matcher = TypeParameterMatcher.find(this, SimplePacketHandler.class, "T");
    }

    @Override
    public final void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            super.channelRead(ctx, msg);
            return;
        }

        ByteBuf buf = (ByteBuf) msg;
        if (buf.getByte(buf.readerIndex()) != packetId) {
            super.channelRead(ctx, msg);
            return;
        }

        try {
            buf.skipBytes(1); // packet id

            int packetLength = buf.readInt();
            if (!buf.isReadable(packetLength)) {
                throw new IllegalArgumentException("Packet buffer is expected to be " + packetLength + " bytes long, but got " + buf.readableBytes() + " bytes!");
            }

            T packet = this.decode(buf.readSlice(packetLength));
            try {
                this.channelRead0(ctx, packet);
            } finally {
                ReferenceCountUtil.release(packet);
            }
        } finally {
            buf.release();
        }
    }

    @Override
    public final void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!this.matcher.match(msg)) {
            super.write(ctx, msg, promise);
            return;
        }

        T packet = (T) msg;
        try {
            ByteBuf packetBuffer = ctx.channel().alloc().ioBuffer();
            try {
                this.encode(packet, packetBuffer);

                ByteBuf buffer = ctx.channel().alloc().ioBuffer();
                buffer.writeByte(this.packetId);
                buffer.writeInt(packetBuffer.readableBytes());
                buffer.writeBytes(packetBuffer, packetBuffer.readableBytes());
                ctx.write(buffer, promise);
            } finally {
                packetBuffer.release();
            }
        } finally {
            ReferenceCountUtil.release(packet);
        }
    }

    protected void channelRead0(ChannelHandlerContext ctx, T packet) {
        ctx.fireChannelRead(ReferenceCountUtil.retain(packet));
    }

    protected abstract T decode(ByteBuf buffer) throws Exception;

    protected abstract void encode(T packet, ByteBuf buffer) throws Exception;
}
