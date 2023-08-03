package eu.mizerak.alemiz.rakperf.channel;

import eu.mizerak.alemiz.rakperf.channel.data.ConnectionSettings;
import eu.mizerak.alemiz.rakperf.client.RakPerfClient;
import io.netty.channel.Channel;

public class RakPerfClientInitializer extends RakPerfInitializer {

    private final ConnectionSettings settings;
    private final int payloadSize;

    public RakPerfClientInitializer(ConnectionSettings settings, int payloadSize) {
        this.settings = settings;
        this.payloadSize = payloadSize;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        /*channel.pipeline().addLast(new ChannelOutboundHandlerAdapter(){
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                System.out.println("write " + msg);
                super.write(ctx, msg, promise);
            }
        });*/

        super.initChannel(channel);
        channel.pipeline().addLast(new RakPerfClient(channel, this.settings, this.payloadSize));
    }
}
