package org.starling.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Habbo V26 Variable-Length 64 integer encoding.
 *
 * First byte: [1][count(3)][sign(1)][data(2)]
 *   - bit 6: always 1 (B64 mask)
 *   - bits 5-3: total byte count (1-5)
 *   - bit 2: sign (1 = negative)
 *   - bits 1-0: first 2 data bits
 *
 * Subsequent bytes: [1][data(6)]
 *   - bit 6: always 1
 *   - bits 5-0: next 6 data bits
 */
public final class VL64Encoding {

    private static final int B64 = 64;

    /**
     * Creates a new VL64Encoding.
     */
    private VL64Encoding() {}

    /**
     * Encodes.
     * @param value the value value
     * @return the resulting encode
     */
    public static byte[] encode(int value) {
        boolean negative = value < 0;
        int abs = Math.abs(value);

        int firstData = abs & 3;
        int remaining = abs >> 2;
        int count = 1;

        // Count how many extra bytes we need
        int temp = remaining;
        while (temp != 0) {
            count++;
            temp >>= 6;
        }

        byte[] result = new byte[count];

        // First byte: B64 | (count << 3) | (sign << 2) | first 2 bits
        result[0] = (byte) (B64 | (count << 3) | (negative ? 4 : 0) | firstData);

        // Subsequent bytes: B64 | 6 data bits
        for (int i = 1; i < count; i++) {
            result[i] = (byte) (B64 | (remaining & 63));
            remaining >>= 6;
        }

        return result;
    }

    /**
     * Decodes.
     * @param buf the buf value
     * @return the resulting decode
     */
    public static int decode(ByteBuf buf) {
        int firstByte = buf.readByte() & 63; // strip B64 mask
        int count = (firstByte >> 3) & 7;
        boolean negative = (firstByte & 4) != 0;
        int value = firstByte & 3;

        int multiplier = 4; // 2^2, since first byte holds 2 data bits
        for (int i = 1; i < count; i++) {
            int nextByte = buf.readByte() & 63;
            value += nextByte * multiplier;
            multiplier <<= 6; // *= 64
        }

        return negative ? -value : value;
    }

    /**
     * Returns the number of bytes a VL64-encoded integer occupies,
     * by peeking at the first byte without advancing the reader index.
     */
    public static int peekSize(ByteBuf buf) {
        int firstByte = buf.getByte(buf.readerIndex()) & 63;
        return (firstByte >> 3) & 7;
    }
}
