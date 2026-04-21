package org.oldskooler.vibe.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.vibe.crypto.HabboCipher;
import org.oldskooler.vibe.net.GameChannelPipeline;
import org.oldskooler.vibe.net.session.Session;

import java.util.List;

/**
 * Decrypts the Director-style init handshake transport before
 * {@link GameDecoder} sees the plaintext packet bytes.
 */
public class InboundCryptoDecoder extends ByteToMessageDecoder {

    private static final Logger log = LogManager.getLogger(InboundCryptoDecoder.class);
    private static final int ENCRYPTED_LENGTH_HEX_BYTES = 6;
    private static final int PLAINTEXT_LENGTH_BYTES = 3;

    /**
     * Decodes.
     * @param ctx the ctx value
     * @param in the in value
     * @param out the out value
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        Session session = ctx.channel().attr(Session.KEY).get();
        if (session == null) {
            removeSelf(ctx);
            passThrough(in, out);
            return;
        }

        HabboCipher cipher = session.getInboundCipher();
        if (!session.isInboundEncrypted() || cipher == null) {
            fallbackToPlaintext(session, in, out, null);
            return;
        }

        while (true) {
            if (in.readableBytes() < ENCRYPTED_LENGTH_HEX_BYTES) {
                if (in.readableBytes() >= PLAINTEXT_LENGTH_BYTES && !looksLikeHex(in, in.readableBytes())) {
                    fallbackToPlaintext(
                            session,
                            in,
                            out,
                            "Session {} stayed in plaintext after crypto setup, falling back to plaintext decoding"
                    );
                }
                return;
            }

            if (!looksLikeHex(in, ENCRYPTED_LENGTH_HEX_BYTES)) {
                fallbackToPlaintext(
                        session,
                        in,
                        out,
                        "Session {} sent non-hex data after crypto setup, falling back to plaintext decoding"
                );
                return;
            }

            in.markReaderIndex();

            byte[] encryptedLength = new byte[ENCRYPTED_LENGTH_HEX_BYTES];
            in.readBytes(encryptedLength);

            byte[] decryptedLength = cipher.copy().decryptHexStream(encryptedLength);
            if (!isHabboBase64Prefix(decryptedLength)) {
                log.warn("Invalid encrypted length prefix for {}",
                        session.getRemoteAddress());
                return;
            }

            int bodyLength = Base64Encoding.decodeLength(
                    decryptedLength[0],
                    decryptedLength[1],
                    decryptedLength[2]
            );
            if (bodyLength < 2) {
                log.warn("Invalid encrypted message length {} for {}",
                        bodyLength,
                        session.getRemoteAddress());
                return;
            }

            int encryptedBodyHexBytes = bodyLength * 2;
            if (in.readableBytes() < encryptedBodyHexBytes) {
                in.resetReaderIndex();
                return;
            }

            byte[] encryptedFrame = new byte[ENCRYPTED_LENGTH_HEX_BYTES + encryptedBodyHexBytes];
            System.arraycopy(encryptedLength, 0, encryptedFrame, 0, encryptedLength.length);
            in.readBytes(encryptedFrame, encryptedLength.length, encryptedBodyHexBytes);

            out.add(Unpooled.wrappedBuffer(cipher.decryptFrame(encryptedFrame)));
        }
    }

    /**
     * Removes self.
     * @param ctx the ctx value
     */
    private void removeSelf(ChannelHandlerContext ctx) {
        if (ctx.pipeline().context(this) != null) {
            ctx.pipeline().remove(this);
        }
    }

    /**
     * Falls back to plaintext.
     * @param session the session value
     * @param in the in value
     * @param out the out value
     * @param reason the reason value
     */
    private void fallbackToPlaintext(Session session, ByteBuf in, List<Object> out, String reason) {
        GameChannelPipeline.disableInboundCrypto(session);
        if (reason != null) {
            log.warn(reason, session.getRemoteAddress());
        }
        passThrough(in, out);
    }

    /**
     * Passes through.
     * @param in the in value
     * @param out the out value
     */
    private void passThrough(ByteBuf in, List<Object> out) {
        if (in.isReadable()) {
            out.add(in.readRetainedSlice(in.readableBytes()));
        }
    }

    /**
     * Lookses like hex.
     * @param in the in value
     * @param bytesToCheck the bytes to check value
     * @return the result of this operation
     */
    private static boolean looksLikeHex(ByteBuf in, int bytesToCheck) {
        int readerIndex = in.readerIndex();
        for (int i = 0; i < bytesToCheck; i++) {
            byte value = in.getByte(readerIndex + i);
            if (!isHex(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ises hex.
     * @param value the value value
     * @return the result of this operation
     */
    private static boolean isHex(byte value) {
        return (value >= '0' && value <= '9')
                || (value >= 'A' && value <= 'F')
                || (value >= 'a' && value <= 'f');
    }

    /**
     * Ises habbo base64 prefix.
     * @param value the value value
     * @return the result of this operation
     */
    private static boolean isHabboBase64Prefix(byte[] value) {
        return value.length >= PLAINTEXT_LENGTH_BYTES
                && isHabboBase64Byte(value[0])
                && isHabboBase64Byte(value[1])
                && isHabboBase64Byte(value[2]);
    }

    /**
     * Ises habbo base64 byte.
     * @param value the value value
     * @return the result of this operation
     */
    private static boolean isHabboBase64Byte(byte value) {
        int unsigned = value & 0xFF;
        return unsigned >= 64 && unsigned <= 127;
    }
}
