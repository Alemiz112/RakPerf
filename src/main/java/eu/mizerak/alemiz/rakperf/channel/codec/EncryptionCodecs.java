package eu.mizerak.alemiz.rakperf.channel.codec;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.protocol.bedrock.netty.codec.encryption.BedrockEncryptionDecoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.encryption.BedrockEncryptionEncoder;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import javax.crypto.SecretKey;
import java.util.Objects;

@Slf4j
public class EncryptionCodecs {

    public static void init(Channel channel, SecretKey secretKey) {
        Objects.requireNonNull(secretKey, "secretKey");
        if (!secretKey.getAlgorithm().equals("AES")) {
            throw new IllegalArgumentException("Invalid key algorithm");
        }

        if (channel.pipeline().get(BedrockEncryptionEncoder.class) != null || channel.pipeline().get(BedrockEncryptionDecoder.class) != null) {
            throw new IllegalStateException("Encryption is already enabled");
        }

        channel.pipeline().addAfter(RakMessageCodec.NAME, "encryption-encoder", new BedrockEncryptionEncoder(secretKey, EncryptionUtils.createCipher(true, true, secretKey)));
        channel.pipeline().addAfter(RakMessageCodec.NAME, "encryption-decoder", new BedrockEncryptionDecoder(secretKey, EncryptionUtils.createCipher(true, false, secretKey)));
        log.info("Encryption enabled for {}", channel.remoteAddress());
    }
}
