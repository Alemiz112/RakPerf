package eu.mizerak.alemiz.rakperf.channel.codec;

import eu.mizerak.alemiz.rakperf.channel.data.ConnectionSettings;
import io.netty.channel.Channel;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SnappyCompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.ZlibCompressionCodec;
import org.cloudburstmc.protocol.common.util.Zlib;

public class CompressionCodecs {

    public static void init(Channel channel, ConnectionSettings.Compression compression) {
        switch (compression) {
            case ZLIB:
                ZlibCompressionCodec codec = new ZlibCompressionCodec(Zlib.RAW);
                codec.setLevel(7);
                channel.pipeline().addAfter(RakMessageCodec.NAME,
                        "compression", codec);
                break;
            case SNAPPY:
                channel.pipeline().addAfter(RakMessageCodec.NAME,
                        "compression", new SnappyCompressionCodec());
                break;
        }
    }
}
