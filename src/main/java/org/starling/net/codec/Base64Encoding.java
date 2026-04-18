package org.starling.net.codec;

import io.netty.buffer.ByteBuf;

/**
 * Habbo V26 "Base64" encoding. Every byte on the wire has bit 6 set (OR'd with 64).
 * Only 6 data bits per byte. Used for opcode headers, length prefixes, and string lengths.
 */
public final class Base64Encoding {

    private static final int MASK = 64;

    /**
     * Creates a new Base64Encoding.
     */
    private Base64Encoding() {}

    // --- 2-byte opcode header (big-endian) ---

    /**
     * Decodes header.
     * @param hi the hi value
     * @param lo the lo value
     * @return the resulting decode header
     */
    public static int decodeHeader(byte hi, byte lo) {
        return ((hi & 63) << 6) | (lo & 63);
    }

    /**
     * Encodes header.
     * @param opcode the opcode value
     * @return the resulting encode header
     */
    public static byte[] encodeHeader(int opcode) {
        return new byte[]{
                (byte) (MASK | ((opcode >> 6) & 63)),
                (byte) (MASK | (opcode & 63))
        };
    }

    // --- 3-byte length prefix (big-endian) ---

    /**
     * Decodes length.
     * @param hi the hi value
     * @param mid the mid value
     * @param lo the lo value
     * @return the resulting decode length
     */
    public static int decodeLength(byte hi, byte mid, byte lo) {
        return ((hi & 63) << 12) | ((mid & 63) << 6) | (lo & 63);
    }

    /**
     * Encodes length.
     * @param length the length value
     * @return the resulting encode length
     */
    public static byte[] encodeLength(int length) {
        return new byte[]{
                (byte) (MASK | ((length >> 12) & 63)),
                (byte) (MASK | ((length >> 6) & 63)),
                (byte) (MASK | (length & 63))
        };
    }

    // --- 2-byte B64 short value ---

    /**
     * Decodes short.
     * @param hi the hi value
     * @param lo the lo value
     * @return the resulting decode short
     */
    public static int decodeShort(byte hi, byte lo) {
        return ((hi & 63) << 6) | (lo & 63);
    }

    /**
     * Encodes short.
     * @param value the value value
     * @return the resulting encode short
     */
    public static byte[] encodeShort(int value) {
        return new byte[]{
                (byte) (MASK | ((value >> 6) & 63)),
                (byte) (MASK | (value & 63))
        };
    }

    // --- Reading from ByteBuf ---

    /**
     * Reads header.
     * @param buf the buf value
     * @return the resulting read header
     */
    public static int readHeader(ByteBuf buf) {
        byte hi = buf.readByte();
        byte lo = buf.readByte();
        return decodeHeader(hi, lo);
    }

    /**
     * Reads b64 short.
     * @param buf the buf value
     * @return the resulting read b64 short
     */
    public static int readB64Short(ByteBuf buf) {
        byte hi = buf.readByte();
        byte lo = buf.readByte();
        return decodeShort(hi, lo);
    }

    /**
     * Reads b64 string.
     * @param buf the buf value
     * @return the resulting read b64 string
     */
    public static String readB64String(ByteBuf buf) {
        int length = readB64Short(buf);
        if (length == 0) return "";
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
