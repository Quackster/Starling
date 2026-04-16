package org.starling.message.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.starling.crypto.DiffieHellman;
import org.starling.crypto.HabboCipher;
import org.starling.crypto.SecretKeyCodec;
import org.starling.message.IncomingPackets;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.Base64Encoding;
import org.starling.net.codec.ClientMessage;
import org.starling.net.codec.GameEncoder;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;

import java.math.BigInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandshakeCryptoFlowTest {

    private static final String ENCODED_SECRET_KEY = "a0b1c2d3a0b1c2d3";
    private static final BigInteger INIT_PRIME = new BigInteger(
            "A8EA077D4943CC98E53C21F5F7C7A0DB8BCE7506F8361A7C1690392F2B090C96" +
            "EE8BC67BAA0DCB7183F16401F5CB838E3B6EE86B9EF2E5D0F3C49D4DC4EDC2B9", 16);
    private static final BigInteger INIT_GENERATOR = BigInteger.valueOf(5L);

    @Test
    void secretDecodeMatchesDirectorAlgorithm() {
        assertEquals(69155, SecretKeyCodec.secretDecode(ENCODED_SECRET_KEY));
    }

    @Test
    void initCryptoDisablesServerToClientEncryption() {
        EmbeddedChannel channel = new EmbeddedChannel(new GameEncoder());
        Session session = new Session(channel);
        channel.attr(Session.KEY).set(session);

        ClientMessage message = new ClientMessage(IncomingPackets.INIT_CRYPTO, Unpooled.EMPTY_BUFFER);
        HandshakeHandlers.handleInitCrypto(session, message);

        assertArrayEquals(
                new ServerMessage(OutgoingPackets.CRYPTO_PARAMETERS).writeInt(0).toBytes(),
                readOutboundBytes(channel)
        );

        channel.finishAndReleaseAll();
    }

    @Test
    void secretKeyEnablesEncryptedEndOfCryptoParams() {
        assertEncryptedEndOfCrypto();
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

    @Test
    void generateKeyResponseIsDirectorCompatibleForInitHandshake() {
        EmbeddedChannel channel = new EmbeddedChannel(new GameEncoder());
        Session session = new Session(channel);
        channel.attr(Session.KEY).set(session);

        BigInteger clientPrivateKey = new BigInteger(
                "123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", 16);
        String clientPublicKey = INIT_GENERATOR.modPow(clientPrivateKey, INIT_PRIME).toString(16);

        ClientMessage message = stringMessage(IncomingPackets.GENERATEKEY, clientPublicKey);
        try {
            HandshakeHandlers.handleGenerateKey(session, message);
        } finally {
            message.release();
        }

        byte[] serverSecretPacket = readOutboundBytes(channel);
        String serverPublicKeyWire = extractServerBody(serverSecretPacket);

        byte[] directorSharedKey = computeSharedSecret(serverPublicKeyWire, clientPrivateKey, INIT_PRIME);
        byte[] plaintext = clientFrame(IncomingPackets.SECRETKEY, encodeStringBody(ENCODED_SECRET_KEY));

        HabboCipher directorEncoder = new HabboCipher();
        directorEncoder.initInitSocket(directorSharedKey);
        byte[] encryptedFrame = directorEncoder.encryptToHex(plaintext);

        assertArrayEquals(plaintext, session.getInboundCipher().copy().decryptFrame(encryptedFrame));

        channel.finishAndReleaseAll();
    }

    @Test
    void initDiffieHellmanMatchesDirectorPublicKeyRequirements() {
        for (int i = 0; i < 32; i++) {
            String publicKeyHex = DiffieHellman.init().getPublicKeyHex();
            assertTrue(publicKeyHex.length() >= 72, "expected init public key length >= 72, got " + publicKeyHex.length());
            assertTrue(publicKeyHex.matches("[0-9A-F]+"), "expected uppercase init hex public key");
        }
    }

    private static void assertEncryptedEndOfCrypto() {
        EmbeddedChannel channel = new EmbeddedChannel(new GameEncoder());
        Session session = new Session(channel);
        channel.attr(Session.KEY).set(session);
        session.setCryptoMode(Session.CryptoMode.INIT);

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
        clientDecoder.initServerToClientSecretKey(secretKey);

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

    private static String extractServerBody(byte[] packet) {
        return new String(packet, 2, packet.length - 3, UTF_8);
    }

    private static byte[] computeSharedSecret(String otherPublicKeyHex, BigInteger privateKey, BigInteger prime) {
        BigInteger otherPublicKey = new BigInteger(otherPublicKeyHex, 16);
        BigInteger sharedSecret = otherPublicKey.modPow(privateKey, prime);
        String sharedHex = sharedSecret.toString(16);
        if ((sharedHex.length() & 1) != 0) {
            sharedHex = "0" + sharedHex;
        }

        byte[] sharedBytes = new byte[sharedHex.length() / 2];
        for (int i = 0; i < sharedBytes.length; i++) {
            sharedBytes[i] = (byte) Integer.parseInt(sharedHex.substring(i * 2, (i * 2) + 2), 16);
        }
        return sharedBytes;
    }
}
