package org.starling.support;

public final class PacketDebugStrings {

    private PacketDebugStrings() {}

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
