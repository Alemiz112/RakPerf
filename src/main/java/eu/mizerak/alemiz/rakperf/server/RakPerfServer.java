package eu.mizerak.alemiz.rakperf.server;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RakPerfServer  {

    public static final AttributeKey<RakPerfServer> ATTRIBUTE = AttributeKey.newInstance("rakperf_server");

    private final Channel channel;

    public RakPerfServer(Channel channel) {
        this.channel = channel;
        channel.attr(ATTRIBUTE).set(this);
    }
}
