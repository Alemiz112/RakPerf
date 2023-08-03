package eu.mizerak.alemiz.rakperf.channel;

import eu.mizerak.alemiz.rakperf.channel.codec.RakMessageCodec;
import eu.mizerak.alemiz.rakperf.channel.codec.handler.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public abstract class RakPerfInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel channel) throws Exception {
        channel.pipeline()
                .addLast(RakMessageCodec.NAME, RakMessageCodec.INSTANCE)
                .addLast(new ConnectionInitPacketHandler())
                .addLast(new ServerHandshakePacketHandler())
                .addLast(new PayloadPacketHandler());
    }
}
