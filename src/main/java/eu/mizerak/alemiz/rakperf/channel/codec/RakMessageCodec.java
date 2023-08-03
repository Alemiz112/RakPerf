package eu.mizerak.alemiz.rakperf.channel.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.cloudburstmc.netty.channel.raknet.RakReliability;
import org.cloudburstmc.netty.channel.raknet.packet.RakMessage;

import java.util.List;

@ChannelHandler.Sharable
public class RakMessageCodec extends MessageToMessageCodec<RakMessage, ByteBuf> {
    public static final String NAME = "rak-msg-codec";
    public static final RakMessageCodec INSTANCE = new RakMessageCodec();

    private static final byte FRAME_ID = (byte) 0xfa;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        CompositeByteBuf buf = ctx.alloc().compositeDirectBuffer(2);

        try {
            buf.addComponent(true, ctx.alloc().ioBuffer(1).writeByte(FRAME_ID));
            buf.addComponent(true, msg.retainedSlice());
            out.add(buf.retain());
        } finally {
            buf.release();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, RakMessage msg, List<Object> out) {
        if (msg.channel() == 0 || msg.reliability() == RakReliability.RELIABLE_ORDERED) {
            ByteBuf in = msg.content();
            if (in.isReadable()) {
                byte id = in.readByte();
                if (id == FRAME_ID) {
                    out.add(in.readRetainedSlice(in.readableBytes()));
                } else {
                    throw new IllegalArgumentException("Unexpected frame received");
                }
            }
        }
    }
}
