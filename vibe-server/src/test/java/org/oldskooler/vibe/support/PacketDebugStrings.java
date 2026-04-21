package org.oldskooler.vibe.support;

public final class PacketDebugStrings {

    /**
     * Creates a new PacketDebugStrings.
     */
    private PacketDebugStrings() {}

    /**
     * Describes.
     * @param bytes the bytes value
     * @return the result of this operation
     */
    public static String describe(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 3);
        for (byte value : bytes) {
            int unsigned = value & 0xFF;
            if (unsigned >= 32 && unsigned <= 126) {
                builder.append((char) unsigned);
            } else {
                builder.append('[').append(unsigned).append(']');
            }
        }
        return builder.toString();
    }
}
