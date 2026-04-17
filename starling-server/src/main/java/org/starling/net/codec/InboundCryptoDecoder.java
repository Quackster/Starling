package org.starling.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.crypto.HabboCipher;
import org.starling.message.IncomingPackets;
import org.starling.net.GameChannelPipeline;
import org.starling.net.session.Session;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Decrypts the Director-style init handshake transport.
 * It sits before {@link GameDecoder} and converts encrypted hex frames back
 * into their plaintext Habbo packet frames.
 */
public class InboundCryptoDecoder extends ByteToMessageDecoder {

    private static final Logger log = LogManager.getLogger(InboundCryptoDecoder.class);
    private static final int FIXED_DH_KEY_BYTES = 64;

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
            GameChannelPipeline.disableInboundCrypto(session);
            passThrough(in, out);
            return;
        }

        while (true) {
            if (in.readableBytes() < 6) {
                if (in.readableBytes() > 0 && log.isDebugEnabled()) {
                    log.debug("Encrypted session {} buffered {} hex chars, waiting for frame prefix",
                            session.getRemoteAddress(),
                            in.readableBytes());
                }
                if (in.readableBytes() >= 3 && !looksLikeHex(in, in.readableBytes())) {
                    log.warn("Session {} stayed in plaintext after crypto setup, disabling encrypted decoding",
                            session.getRemoteAddress());
                    GameChannelPipeline.disableInboundCrypto(session);
                    passThrough(in, out);
                }
                return;
            }

            if (!looksLikeHex(in, 6)) {
                log.warn("Session {} sent non-hex data after crypto setup, falling back to plaintext compatibility",
                        session.getRemoteAddress());
                GameChannelPipeline.disableInboundCrypto(session);
                passThrough(in, out);
                return;
            }

            int frameHexLength = 6;
            in.markReaderIndex();

            byte[] lengthHex = new byte[6];
            in.readBytes(lengthHex);

            byte[] decryptedLength = cipher.copy().decryptHexStream(lengthHex);
            int bodyLength = Base64Encoding.decodeLength(decryptedLength[0], decryptedLength[1], decryptedLength[2]);

            if (bodyLength < 2) {
                log.warn(
                        "Invalid encrypted message length {} for {} (mode={}, prefixHex={}, prefixPlain={})",
                        bodyLength,
                        session.getRemoteAddress(),
                        session.getCryptoMode(),
                        formatWire(lengthHex),
                        formatWire(decryptedLength)
                );
                logEncryptedDiagnostics(session, in, cipher, lengthHex);
                return;
            }

            int bodyHexLength = bodyLength * 2;
            if (in.readableBytes() < bodyHexLength) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Encrypted session {} awaiting full frame: bodyLen={} needHex={} availableHex={} prefixHex={} prefixPlain={}",
                            session.getRemoteAddress(),
                            bodyLength,
                            bodyHexLength,
                            in.readableBytes(),
                            formatWire(lengthHex),
                            formatWire(decryptedLength)
                    );
                }
                if (!isHabboBase64Prefix(decryptedLength) || bodyLength > 4096) {
                    logEncryptedDiagnostics(session, in, cipher, lengthHex);
                }
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
                    log.warn(
                            "Encrypted length mismatch for {} (mode={}): preview={}, frame={}, prefixHex={}, prefixPlain={}, frameHead={}",
                            session.getRemoteAddress(),
                            session.getCryptoMode(),
                            bodyLength,
                            decodedLength,
                            formatWire(lengthHex),
                            formatWire(decryptedLength),
                            formatWire(prefix(decryptedFrame, 12))
                    );
                }
            } finally {
                frameBuf.release();
            }

            out.add(Unpooled.wrappedBuffer(decryptedFrame));
        }
    }

    private void removeSelf(ChannelHandlerContext ctx) {
        if (ctx.pipeline().context(this) != null) {
            ctx.pipeline().remove(this);
        }
    }

    private void passThrough(ByteBuf in, List<Object> out) {
        if (in.isReadable()) {
            out.add(in.readRetainedSlice(in.readableBytes()));
        }
    }

    private void logEncryptedDiagnostics(Session session, ByteBuf in, HabboCipher initCipher, byte[] lengthHex) {
        if (!log.isDebugEnabled()) {
            return;
        }

        int extraHexChars = Math.min(in.readableBytes(), 42);
        byte[] rawHead = new byte[lengthHex.length + extraHexChars];
        System.arraycopy(lengthHex, 0, rawHead, 0, lengthHex.length);
        if (extraHexChars > 0) {
            in.getBytes(in.readerIndex(), rawHead, lengthHex.length, extraHexChars);
        }

        int previewHexChars = Math.min(rawHead.length, 24);
        previewHexChars -= previewHexChars & 1;
        if (previewHexChars < 6) {
            previewHexChars = 6;
        }

        byte[] rawPreview = Arrays.copyOf(rawHead, previewHexChars);
        byte[] initPreview = initCipher.copy().decryptHexStream(rawPreview);
        String initShifts = describeShiftCandidates(rawHead, initCipher);
        String hypothesisSummary = describeSharedKeyHypotheses(rawHead, session.getInboundSharedSecret());
        String familySummary = describeCipherFamilies(rawHead, session.getInboundSharedSecret());

        if (!session.markEncryptedDiagnosticsContextLogged()) {
            log.debug(
                    "Encrypted context for {} clientPublicKeyHex={} serverPrivateKeyHex={} serverPublicKeyHex={} sharedSecretHex={}",
                    session.getRemoteAddress(),
                    safe(session.getDebugClientPublicKeyHex()),
                    safe(session.getDebugServerPrivateKeyHex()),
                    safe(session.getDebugServerPublicKeyHex()),
                    safe(session.getDebugSharedSecretHex())
            );
        }

        log.debug(
                "Encrypted diagnostics for {} rawHead={} initState={} initPlain={} initShifts={}",
                session.getRemoteAddress(),
                formatWire(rawHead),
                initCipher.debugStateSummary(8),
                formatWire(initPreview),
                initShifts
        );
        if (!hypothesisSummary.equals("n/a")) {
            log.debug("Encrypted hypotheses for {} {}", session.getRemoteAddress(), hypothesisSummary);
        }
        if (!familySummary.equals("n/a")) {
            log.debug("Encrypted families for {} {}", session.getRemoteAddress(), familySummary);
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

    private static boolean isHabboBase64Prefix(byte[] value) {
        return value.length >= 3
                && isHabboBase64Byte(value[0])
                && isHabboBase64Byte(value[1])
                && isHabboBase64Byte(value[2]);
    }

    private static boolean isHabboBase64Byte(byte value) {
        int unsigned = value & 0xFF;
        return unsigned >= 64 && unsigned <= 127;
    }

    private static String describeSharedKeyHypotheses(byte[] rawHead, byte[] sharedSecret) {
        if (sharedSecret == null || sharedSecret.length == 0) {
            return "n/a";
        }

        StringBuilder summary = new StringBuilder();
        Set<String> seen = new LinkedHashSet<>();

        appendHypothesis(summary, seen, "raw", rawHead, sharedSecret);
        appendHypothesis(summary, seen, "pad64", rawHead, leftPad(sharedSecret, FIXED_DH_KEY_BYTES));
        appendHypothesis(summary, seen, "rev", rawHead, reverse(sharedSecret));
        if ((sharedSecret[0] & 0x80) != 0) {
            appendHypothesis(summary, seen, "signPad", rawHead, prependZero(sharedSecret));
        }

        String sharedHexUpper = toHex(sharedSecret, true);
        appendHypothesis(summary, seen, "hexU", rawHead, sharedHexUpper.getBytes(StandardCharsets.US_ASCII));
        appendHypothesis(summary, seen, "hexL", rawHead, sharedHexUpper.toLowerCase().getBytes(StandardCharsets.US_ASCII));

        return summary.length() == 0 ? "n/a" : summary.toString();
    }

    private static String describeCipherFamilies(byte[] rawHead, byte[] sharedSecret) {
        if (sharedSecret == null || sharedSecret.length == 0) {
            return "n/a";
        }

        return String.join(" ",
                describeCipherFamily("init", rawHead, cipherForInit(sharedSecret)),
                describeCipherFamily("init0", rawHead, cipherForInitNoPremix(sharedSecret)),
                describeCipherFamily("std", rawHead, cipherForStandard(sharedSecret)),
                describeCipherFamily("xstd", rawHead, cipherForStandardXored(sharedSecret))
        );
    }

    private static String describeShiftCandidates(byte[] rawHead, HabboCipher cipher) {
        int maxShiftBytes = Math.min(3, (rawHead.length / 2) - 3);
        if (maxShiftBytes < 0) {
            return "n/a";
        }

        StringBuilder summary = new StringBuilder();
        for (int shiftBytes = 0; shiftBytes <= maxShiftBytes; shiftBytes++) {
            int previewHexChars = (shiftBytes + 3) * 2;
            byte[] preview = cipher.copy().decryptHexStream(Arrays.copyOf(rawHead, previewHexChars));
            byte[] prefix = Arrays.copyOfRange(preview, preview.length - 3, preview.length);

            if (shiftBytes > 0) {
                summary.append(' ');
            }

            summary.append(shiftBytes)
                    .append(':')
                    .append(formatWire(prefix))
                    .append('/')
                    .append(isHabboBase64Prefix(prefix)
                            ? Base64Encoding.decodeLength(prefix[0], prefix[1], prefix[2])
                            : "invalid");
        }
        return summary.toString();
    }

    private static void appendHypothesis(
            StringBuilder summary,
            Set<String> seen,
            String label,
            byte[] rawHead,
            byte[] candidateKey
    ) {
        if (candidateKey == null || candidateKey.length == 0) {
            return;
        }

        String fingerprint = toHex(candidateKey, true);
        if (!seen.add(fingerprint)) {
            return;
        }

        if (summary.length() > 0) {
            summary.append(' ');
        }
        summary.append(describeHypothesis(label, rawHead, candidateKey));
    }

    private static String describeHypothesis(String label, byte[] rawHead, byte[] candidateKey) {
        HabboCipher cipher = cipherForInit(candidateKey);

        int previewHexChars = Math.min(rawHead.length, 10);
        previewHexChars -= previewHexChars & 1;
        if (previewHexChars < 6) {
            previewHexChars = 6;
        }

        byte[] preview = cipher.copy().decryptHexStream(Arrays.copyOf(rawHead, previewHexChars));
        byte[] prefix = Arrays.copyOf(preview, Math.min(preview.length, 3));

        StringBuilder description = new StringBuilder(label)
                .append('=')
                .append(formatWire(prefix))
                .append(",head=")
                .append(formatWire(prefix(preview, 5)));

        if (!isHabboBase64Prefix(prefix)) {
            description.append("/invalid");
            return description.toString();
        }

        int bodyLength = Base64Encoding.decodeLength(prefix[0], prefix[1], prefix[2]);
        description.append("/len=").append(bodyLength);

        if (preview.length >= 5
                && isHabboBase64Byte(preview[3])
                && isHabboBase64Byte(preview[4])) {
            int opcode = Base64Encoding.decodeHeader(preview[3], preview[4]);
            description.append(",op=").append(opcode);
            if (isLikelyPostDhOpcode(opcode)) {
                description.append('*');
            }
        } else {
            description.append(",op=?");
        }

        return description.toString();
    }

    private static String describeCipherFamily(String label, byte[] rawHead, HabboCipher cipher) {
        int previewHexChars = Math.min(rawHead.length, 10);
        previewHexChars -= previewHexChars & 1;
        if (previewHexChars < 6) {
            previewHexChars = 6;
        }

        byte[] preview = cipher.copy().decryptHexStream(Arrays.copyOf(rawHead, previewHexChars));
        byte[] prefix = Arrays.copyOf(preview, Math.min(preview.length, 3));

        StringBuilder description = new StringBuilder(label)
                .append('=')
                .append(formatWire(prefix))
                .append(",head=")
                .append(formatWire(prefix(preview, 5)));

        if (!isHabboBase64Prefix(prefix)) {
            description.append("/invalid");
            return description.toString();
        }

        int bodyLength = Base64Encoding.decodeLength(prefix[0], prefix[1], prefix[2]);
        description.append("/len=").append(bodyLength);
        if (preview.length >= 5
                && isHabboBase64Byte(preview[3])
                && isHabboBase64Byte(preview[4])) {
            int opcode = Base64Encoding.decodeHeader(preview[3], preview[4]);
            description.append(",op=").append(opcode);
            if (isLikelyPostDhOpcode(opcode)) {
                description.append('*');
            }
        } else {
            description.append(",op=?");
        }

        return description.toString();
    }

    private static boolean isLikelyPostDhOpcode(int opcode) {
        return opcode == IncomingPackets.VERSIONCHECK
                || opcode == IncomingPackets.UNIQUEID
                || opcode == IncomingPackets.GET_SESSION_PARAMETERS
                || opcode == IncomingPackets.SECRETKEY;
    }

    private static HabboCipher cipherForInit(byte[] key) {
        HabboCipher cipher = new HabboCipher();
        cipher.initInitSocket(key);
        return cipher;
    }

    private static HabboCipher cipherForInitNoPremix(byte[] key) {
        HabboCipher cipher = new HabboCipher();
        cipher.initInitSocketNoPremix(key);
        return cipher;
    }

    private static HabboCipher cipherForStandard(byte[] key) {
        HabboCipher cipher = new HabboCipher();
        cipher.initStandardBytes(key);
        return cipher;
    }

    private static HabboCipher cipherForStandardXored(byte[] key) {
        HabboCipher cipher = new HabboCipher();
        cipher.initStandardXoredShared(key);
        return cipher;
    }

    private static byte[] leftPad(byte[] value, int length) {
        if (value.length >= length) {
            return Arrays.copyOf(value, value.length);
        }

        byte[] padded = new byte[length];
        System.arraycopy(value, 0, padded, length - value.length, value.length);
        return padded;
    }

    private static byte[] prependZero(byte[] value) {
        byte[] padded = new byte[value.length + 1];
        System.arraycopy(value, 0, padded, 1, value.length);
        return padded;
    }

    private static byte[] reverse(byte[] value) {
        byte[] reversed = Arrays.copyOf(value, value.length);
        for (int left = 0, right = reversed.length - 1; left < right; left++, right--) {
            byte temp = reversed[left];
            reversed[left] = reversed[right];
            reversed[right] = temp;
        }
        return reversed;
    }

    private static byte[] prefix(byte[] value, int maxLength) {
        int length = Math.min(value.length, maxLength);
        byte[] prefix = new byte[length];
        System.arraycopy(value, 0, prefix, 0, length);
        return prefix;
    }

    private static String toHex(byte[] value, boolean uppercase) {
        char[] digits = uppercase
                ? new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'}
                : new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] encoded = new char[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            int current = value[i] & 0xFF;
            encoded[i * 2] = digits[(current >>> 4) & 0x0F];
            encoded[(i * 2) + 1] = digits[current & 0x0F];
        }
        return new String(encoded);
    }

    private static String safe(String value) {
        return value == null ? "n/a" : value;
    }

    private static String formatWire(byte[] wireBytes) {
        StringBuilder formatted = new StringBuilder(wireBytes.length * 3);
        for (byte wireByte : wireBytes) {
            int value = wireByte & 0xFF;
            if (value >= 32 && value <= 126) {
                formatted.append((char) value);
            } else {
                formatted.append('[').append(value).append(']');
            }
        }
        return formatted.toString();
    }
}
