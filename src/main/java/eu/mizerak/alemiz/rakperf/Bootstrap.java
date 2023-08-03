package eu.mizerak.alemiz.rakperf;

import eu.mizerak.alemiz.rakperf.channel.RakPerfClientInitializer;
import eu.mizerak.alemiz.rakperf.channel.RakPerfServerInitializer;
import eu.mizerak.alemiz.rakperf.channel.data.ConnectionSettings;
import eu.mizerak.alemiz.rakperf.server.RakPerfServer;
import eu.mizerak.alemiz.rakperf.utils.EncryptionHelper;
import eu.mizerak.alemiz.rakperf.utils.EventLoops;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.util.concurrent.DefaultThreadFactory;
import it.unimi.dsi.fastutil.Pair;
import joptsimple.*;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class Bootstrap {

    public static final int SERVER_PORT = 23191;

    private static final byte[] ADVERTISEMENT = new StringJoiner(";", "", ";")
            .add("MCPE")
            .add("RakPerf")
            .add(Integer.toString(542))
            .add("1.19.0")
            .add(Integer.toString(0))
            .add(Integer.toString(4))
            .add(Long.toUnsignedString(ThreadLocalRandom.current().nextLong()))
            .add("C")
            .add("Survival")
            .add("1")
            .add("19132")
            .add("19132")
            .toString().getBytes(StandardCharsets.UTF_8);

    private static final OptionParser PARSER = new OptionParser();
    static {
        PARSER.allowsUnrecognizedOptions();
        PARSER.formatHelpWith(new BuiltinHelpFormatter(80, 2){
            @Override
            protected String formattedHelpOutput() {
                String heading = "RakPerf is a RakNet performance measuring tool.\n" +
                        "View below for details of the available settings:\n\n";
                return heading + super.formattedHelpOutput();
            }
        });
    }

    private static final OptionSpec<Void> HELP_OPTION = PARSER.acceptsAll(Arrays.asList("?", "h", "help"), "Shows help page")
            .forHelp();

    private static final OptionSpec<String> CLIENT_OPTION = PARSER.acceptsAll(Arrays.asList("c", "client"), "Run in client mode")
            .withRequiredArg()
            .ofType(String.class);

    private static final OptionSpec<Integer> CLIENT_MTU = PARSER.accepts("mtu", "Set client maximum MTU")
            .availableIf("client")
            .withRequiredArg()
            .ofType(Integer.class);

    private static final OptionSpec<Integer> PAYLOAD_SIZE = PARSER.acceptsAll(Arrays.asList("n", "bytes"), "Number of bytes to transmit per packet (default: 1024B)")
            .availableIf("client")
            .withRequiredArg()
            .ofType(Integer.class);

    private static final OptionSpec<Void> ENCRYPTION_FLAG = PARSER.acceptsAll(Arrays.asList("e", "encrypt"), "Set this flag to enable encryption")
            .availableIf("client");

    private static final OptionSpec<Void> BIDIR_FLAG = PARSER.accepts("bidir", "Run in bidirectional mode")
            .availableIf("client");

    private static final OptionSpec<ConnectionSettings.Compression> COMPRESSION_OPTION = PARSER.accepts("compress", "Set compression type")
            .availableIf("client")
            .withRequiredArg()
            .ofType(ConnectionSettings.Compression.class);

    private static final OptionSpec<Void> SERVER_OPTION = PARSER.acceptsAll(Arrays.asList("s", "server"), "Run in server mode");

    private static final OptionSpec<Void> FORCE_NIO_FLAG = PARSER.accepts("nio", "Force usage of NioDatagramChannel");


    public static void main(String[] args) throws Exception {
        OptionSet options;
        if (args.length == 0 || (options = PARSER.parse(args)).has(HELP_OPTION)) {
            PARSER.printHelpOn(System.out);
            return;
        }

        if (!options.has(CLIENT_OPTION) && !options.has(SERVER_OPTION)) {
            PARSER.printHelpOn(System.out);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> Runtime.getRuntime().halt(0)));

        EventLoops.ChannelType channelType = EventLoops.getChannelType();
        if (options.has(FORCE_NIO_FLAG)) {
            channelType = EventLoops.ChannelType.NIO;
        }

        log.info("Using {} channel implementation as default!", channelType);

        boolean serverMode = options.has(SERVER_OPTION);
        if (serverMode) {
            startServer(options, channelType);
        } else {
            startClient(options, channelType);
        }

        new WaitClass();
    }

    private static void startServer(OptionSet options, EventLoops.ChannelType channelType) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap()
                .channelFactory(RakChannelFactory.server(channelType.getDatagramChannel()))
                .group(channelType.newEventLoopGroup(0, new DefaultThreadFactory("rakperf-boss", true, 8)),
                        channelType.newEventLoopGroup(0, new DefaultThreadFactory("rakperf-worker", true, 5)))
                .option(RakChannelOption.RAK_SUPPORTED_PROTOCOLS, new int[]{11})
                .option(RakChannelOption.RAK_MAX_CONNECTIONS, 1)
                .childOption(RakChannelOption.RAK_ORDERING_CHANNELS, 1)
                // TODO: .childOption(RakChannelOption.RAK_METRICS, NetworkMetrics.INSTANCE)
                .option(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong())
                .option(RakChannelOption.RAK_ADVERTISEMENT, Unpooled.wrappedBuffer(ADVERTISEMENT))
                .option(RakChannelOption.RAK_MAX_MTU, 1400)
                .childHandler(new RakPerfServerInitializer());

        if (Epoll.isAvailable()) {
            bootstrap.option(UnixChannelOption.SO_REUSEPORT, true);
        }

        ChannelFuture future = bootstrap.bind(new InetSocketAddress("0.0.0.0", SERVER_PORT)).awaitUninterruptibly();
        if (future.cause() != null) {
            log.error("Unable to start RakPer server", future.cause());
            return;
        }

        RakPerfServer server = new RakPerfServer(future.channel());
        // TODO: attribute maybe?
    }

    private static void startClient(OptionSet options, EventLoops.ChannelType channelType) throws Exception {
        String address = options.valueOf(CLIENT_OPTION);
        int mtu = 1400;
        if (options.has(CLIENT_MTU)) {
            mtu = options.valueOf(CLIENT_MTU);
        }

        ConnectionSettings settings = new ConnectionSettings();
        settings.setCompression(options.valueOf(COMPRESSION_OPTION));
        settings.setBidirectional(options.has(BIDIR_FLAG));

        if (options.has(ENCRYPTION_FLAG)) {
            Pair<KeyPair, byte[]> pair = EncryptionHelper.createEncryptionPair();
            settings.setEncryptionToken(pair.right());
            settings.setEncryptionKey(pair.left());
        }

        int payloadSize = options.has(PAYLOAD_SIZE) ? options.valueOf(PAYLOAD_SIZE) : 1024;
        log.info("Payload packet size will be {}B", payloadSize);

        ChannelFuture future = new io.netty.bootstrap.Bootstrap()
                .channelFactory(RakChannelFactory.client(channelType.getDatagramChannel()))
                .group(channelType.newEventLoopGroup(0, new DefaultThreadFactory("rakperf-worker", true, 5)))
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, 11)
                .option(RakChannelOption.RAK_ORDERING_CHANNELS, 1)
                .option(RakChannelOption.RAK_CONNECT_TIMEOUT, 10000L)
                .option(RakChannelOption.RAK_SESSION_TIMEOUT, 10000L)
                .option(RakChannelOption.RAK_MTU, mtu)
                .handler(new RakPerfClientInitializer(settings, payloadSize))
                .connect(new InetSocketAddress(address, SERVER_PORT))
                .awaitUninterruptibly();

        if (future.cause() != null) {
            log.error("Unable to start RakPer client", future.cause());
            return;
        }
    }

    private static class WaitClass {
        public WaitClass() {
            while (true) {
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
}
