package eu.mizerak.alemiz.rakperf.channel;

import eu.mizerak.alemiz.rakperf.server.RakPerfServer;
import eu.mizerak.alemiz.rakperf.server.RakPerfServerSession;
import io.netty.channel.Channel;

public class RakPerfServerInitializer extends RakPerfInitializer {

    public RakPerfServerInitializer() {
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        /*channel.pipeline().addLast(new ChannelInboundHandlerAdapter(){
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                System.out.println("read " + msg);
                super.channelRead(ctx, msg);
            }
        });*/

        super.initChannel(channel);

        RakPerfServer server = channel.parent().attr(RakPerfServer.ATTRIBUTE).get();
        channel.pipeline().addLast(new RakPerfServerSession(channel, server));
    }
}
