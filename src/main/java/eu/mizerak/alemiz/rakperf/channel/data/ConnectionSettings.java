package eu.mizerak.alemiz.rakperf.channel.data;

import lombok.Data;
import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;

import java.security.KeyPair;

@Data
public class ConnectionSettings {
    private Compression compression;
    private KeyPair encryptionKey;
    private byte[] encryptionToken;
    private boolean bidirectional;

    @Getter
    public enum Compression {
        NONE(null),
        ZLIB(PacketCompressionAlgorithm.ZLIB),
        SNAPPY(PacketCompressionAlgorithm.SNAPPY);

        private final PacketCompressionAlgorithm bedrockCompression;

        Compression(PacketCompressionAlgorithm bedrockCompression) {
            this.bedrockCompression = bedrockCompression;
        }
    }
}
