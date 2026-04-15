package org.starling.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.crypto.HabboCipher;
import org.starling.net.session.Session;

import java.util.List;

/**
 * Decodes client->server messages from the TCP byte stream.
 *
 * Operates in two modes:
 * - PLAINTEXT: Before DH key exchange. Messages are [3-byte B64 length][body]
 * - ENCRYPTED: After DH. Messages are hex-encoded encrypted bytes.
 *   Each original byte is 2 hex chars on the wire.
 *
 * The Session object holds the cipher and the encrypted flag.
 */
public class GameDecoder extends ByteToMessageDecoder {

    private static final Logger log = LogManager.getLogger(GameDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Session session = ctx.channel().attr(Session.KEY).get();
        if (session != null && session.isEncrypted()) {
            decodeEncrypted(in, out, session);
        } else {
            decodePlaintext(in, out);
        }
    }

    private void decodePlaintext(ByteBuf in, List<Object> out) {
        while (in.readableBytes() >= 3) {
            in.markReaderIndex();

            // Read 3-byte B64 length prefix
            byte b0 = in.readByte();
            byte b1 = in.readByte();
            byte b2 = in.readByte();
            int bodyLength = Base64Encoding.decodeLength(b0, b1, b2);

            if (bodyLength < 2) {
                log.warn("Invalid message length: {}", bodyLength);
                return;
            }

            if (in.readableBytes() < bodyLength) {
                in.resetReaderIndex();
                return; // wait for more data
            }

            // Read body: first 2 bytes = header, rest = params
            int opcode = Base64Encoding.readHeader(in);
            int paramsLength = bodyLength - 2;

            ByteBuf paramsBuf;
            if (paramsLength > 0) {
                paramsBuf = in.readRetainedSlice(paramsLength);
            } else {
                paramsBuf = Unpooled.EMPTY_BUFFER;
            }

            out.add(new ClientMessage(opcode, paramsBuf));
        }
    }

    private void decodeEncrypted(ByteBuf in, List<Object> out, Session session) {
        HabboCipher cipher = session.getCipher();
        if (cipher == null) {
            decodePlaintext(in, out);
            return;
        }

        while (true) {
            if (in.readableBytes() < 6) {
                if (in.readableBytes() >= 3 && !looksLikeHex(in, in.readableBytes())) {
                    log.warn("Session {} stayed in plaintext after crypto setup, disabling encrypted decoding",
                            session.getRemoteAddress());
                    session.setEncrypted(false);
                    session.setCipher(null);
                    decodePlaintext(in, out);
                }
                return;
            }

            if (!looksLikeHex(in, 6)) {
                log.warn("Session {} sent non-hex data after crypto setup, falling back to plaintext compatibility",
                        session.getRemoteAddress());
                session.setEncrypted(false);
                session.setCipher(null);
                decodePlaintext(in, out);
                return;
            }

            int frameHexLength = 6;
            in.markReaderIndex();

            byte[] lengthHex = new byte[6];
            in.readBytes(lengthHex);

            byte[] decryptedLength = cipher.copy().decryptFrame(lengthHex);
            int bodyLength = Base64Encoding.decodeLength(decryptedLength[0], decryptedLength[1], decryptedLength[2]);

            if (bodyLength < 2) {
                log.warn("Invalid encrypted message length: {}", bodyLength);
                return;
            }

            int bodyHexLength = bodyLength * 2;
            if (in.readableBytes() < bodyHexLength) {
                in.resetReaderIndex();
                return;
            }

            frameHexLength += bodyHexLength;
            byte[] frameHex = new byte[frameHexLength];
            System.arraycopy(lengthHex, 0, frameHex, 0, lengthHex.length);
            in.readBytes(frameHex, lengthHex.length, bodyHexLength);

            byte[] decryptedFrame = cipher.decryptFrame(frameHex);
            ByteBuf frameBuf = Unpooled.wrappedBuffer(decryptedFrame);
            try {
                int decodedLength = Base64Encoding.decodeLength(
                        frameBuf.readByte(),
                        frameBuf.readByte(),
                        frameBuf.readByte()
                );
                if (decodedLength != bodyLength) {
                    log.debug("Encrypted length mismatch: preview={}, frame={}", bodyLength, decodedLength);
                }

                int opcode = Base64Encoding.readHeader(frameBuf);
                ByteBuf paramsBuf;
                if (frameBuf.isReadable()) {
                    paramsBuf = Unpooled.copiedBuffer(frameBuf);
                } else {
                    paramsBuf = Unpooled.EMPTY_BUFFER;
                }

                out.add(new ClientMessage(opcode, paramsBuf));
            } finally {
                frameBuf.release();
            }
        }
    }

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

    private static boolean isHex(byte value) {
        return (value >= '0' && value <= '9')
                || (value >= 'A' && value <= 'F')
                || (value >= 'a' && value <= 'f');
    }
}
