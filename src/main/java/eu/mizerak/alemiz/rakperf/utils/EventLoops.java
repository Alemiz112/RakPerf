package eu.mizerak.alemiz.rakperf.utils;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.incubator.channel.uring.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;

public class EventLoops {
    private static final ChannelType CHANNEL_TYPE;

    static {
        boolean disableNative = System.getProperties().contains("disableNativeEventLoop");
        boolean allowEpoll = Boolean.parseBoolean(System.getProperty( "bungee.epoll", "true" ));
        boolean allowIoUring = Boolean.parseBoolean(System.getProperty( "bungee.iouring", "false" ));

        if (!disableNative && allowIoUring && IOUring.isAvailable()) {
            CHANNEL_TYPE = ChannelType.IOURING;
        } else if (!disableNative && allowEpoll && Epoll.isAvailable()) {
            CHANNEL_TYPE = ChannelType.EPOLL;
        } else {
            CHANNEL_TYPE = ChannelType.NIO;
        }
    }

    public static ChannelType getChannelType() {
        return CHANNEL_TYPE;
    }

    @Getter
    @RequiredArgsConstructor
    public enum ChannelType {
        IOURING(IOUringDatagramChannel.class, IOUringSocketChannel.class, IOUringServerSocketChannel.class,
                (threads, factory) -> new IOUringEventLoopGroup(threads, factory), IOUring.isAvailable()),
        EPOLL(EpollDatagramChannel.class, EpollSocketChannel.class, EpollServerSocketChannel.class,
                (threads, factory) -> new EpollEventLoopGroup(threads, factory), Epoll.isAvailable()),
        NIO(NioDatagramChannel.class, NioSocketChannel.class, NioServerSocketChannel.class,
                (threads, factory) -> new NioEventLoopGroup(threads, factory), true);

        private final Class<? extends DatagramChannel> datagramChannel;
        private final Class<? extends SocketChannel> socketChannel;
        private final Class<? extends ServerSocketChannel> serverSocketChannel;
        private final BiFunction<Integer, ThreadFactory, EventLoopGroup> eventLoopGroupFactory;
        private final boolean available;

        public EventLoopGroup newEventLoopGroup(int threads, ThreadFactory factory) {
            return this.eventLoopGroupFactory.apply(threads, factory);
        }
    }
}
