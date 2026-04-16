package org.starling.message.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.starling.crypto.HabboCipher;
import org.starling.crypto.SecretKeyCodec;
import org.starling.message.IncomingPackets;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.Base64Encoding;
import org.starling.net.codec.ClientMessage;
import org.starling.net.codec.GameEncoder;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandshakeCryptoFlowTest {

    private static final String ENCODED_SECRET_KEY = "a0b1c2d3a0b1c2d3";

    @Test
    void secretDecodeMatchesDirectorAlgorithm() {
        assertEquals(69155, SecretKeyCodec.secretDecode(ENCODED_SECRET_KEY));
    }

    @Test
    void initCryptoAdvertisesServerToClientEncryption() {
        EmbeddedChannel channel = new EmbeddedChannel(new GameEncoder());
        Session session = new Session(channel);
        channel.attr(Session.KEY).set(session);

        ClientMessage message = new ClientMessage(IncomingPackets.INIT_CRYPTO, Unpooled.EMPTY_BUFFER);
        HandshakeHandlers.handleInitCrypto(session, message);

        assertArrayEquals(
                new ServerMessage(OutgoingPackets.CRYPTO_PARAMETERS).writeInt(1).toBytes(),
                readOutboundBytes(channel)
        );

        channel.finishAndReleaseAll();
    }

    @Test
    void legacySecretKeyEnablesEncryptedEndOfCryptoParams() {
        assertEncryptedEndOfCrypto(Session.CryptoMode.LEGACY);
    }

    @Test
    void initSecretKeyEnablesEncryptedEndOfCryptoParams() {
        assertEncryptedEndOfCrypto(Session.CryptoMode.INIT);
    }

    @Test
    void initSocketCipherCopyKeepsFramePreviewAndFullDecryptInSync() {
        byte[] sharedKey = new byte[]{
                0x01, 0x23, 0x45, 0x67,
                (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
        };
        byte[] plaintext = clientFrame(IncomingPackets.SECRETKEY, encodeStringBody(ENCODED_SECRET_KEY));

        HabboCipher encoder = new HabboCipher();
        encoder.initInitSocket(sharedKey);
        byte[] encryptedFrame = encoder.encryptToHex(plaintext);

        HabboCipher decoder = new HabboCipher();
        decoder.initInitSocket(sharedKey);

        byte[] decryptedPreview = decoder.copy().decryptFrame(new byte[]{
                encryptedFrame[0], encryptedFrame[1],
                encryptedFrame[2], encryptedFrame[3],
                encryptedFrame[4], encryptedFrame[5]
        });
        byte[] decryptedFrame = decoder.decryptFrame(encryptedFrame);

        assertArrayEquals(new byte[]{plaintext[0], plaintext[1], plaintext[2]}, decryptedPreview);
        assertArrayEquals(plaintext, decryptedFrame);
    }

    private static void assertEncryptedEndOfCrypto(Session.CryptoMode cryptoMode) {
        EmbeddedChannel channel = new EmbeddedChannel(new GameEncoder());
        Session session = new Session(channel);
        channel.attr(Session.KEY).set(session);
        session.setCryptoMode(cryptoMode);

        ClientMessage message = stringMessage(IncomingPackets.SECRETKEY, ENCODED_SECRET_KEY);
        try {
            HandshakeHandlers.handleSecretKey(session, message);
        } finally {
            message.release();
        }

        assertTrue(session.isOutboundEncrypted());

        byte[] encryptedBytes = readOutboundBytes(channel);
        HabboCipher clientDecoder = new HabboCipher();
        int secretKey = SecretKeyCodec.secretDecode(ENCODED_SECRET_KEY);
        clientDecoder.initLegacyServerToClient(secretKey);

        assertArrayEquals(
                new ServerMessage(OutgoingPackets.END_OF_CRYPTO_PARAMS).toBytes(),
                clientDecoder.decryptFrame(encryptedBytes)
        );

        channel.finishAndReleaseAll();
    }

    private static ClientMessage stringMessage(int opcode, String value) {
        byte[] encodedBody = encodeStringBody(value);
        ByteBuf body = Unpooled.buffer(encodedBody.length);
        body.writeBytes(encodedBody);
        return new ClientMessage(opcode, body);
    }

    private static byte[] encodeStringBody(String value) {
        byte[] bytes = value.getBytes(UTF_8);
        ByteBuf body = Unpooled.buffer(bytes.length + 2);
        try {
            body.writeBytes(Base64Encoding.encodeShort(bytes.length));
            body.writeBytes(bytes);
            byte[] encoded = new byte[body.readableBytes()];
            body.getBytes(body.readerIndex(), encoded);
            return encoded;
        } finally {
            body.release();
        }
    }

    private static byte[] clientFrame(int opcode, byte[] body) {
        byte[] header = Base64Encoding.encodeHeader(opcode);
        byte[] length = Base64Encoding.encodeLength(header.length + body.length);
        byte[] frame = new byte[length.length + header.length + body.length];
        System.arraycopy(length, 0, frame, 0, length.length);
        System.arraycopy(header, 0, frame, length.length, header.length);
        System.arraycopy(body, 0, frame, length.length + header.length, body.length);
        return frame;
    }

    private static byte[] readOutboundBytes(EmbeddedChannel channel) {
        ByteBuf buffer = channel.readOutbound();
        try {
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }
}
