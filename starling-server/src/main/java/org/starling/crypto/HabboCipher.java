package org.starling.crypto;

import java.nio.charset.StandardCharsets;

/**
 * Habbo V26 stream cipher wrapper.
 *
 * Supports both login variants present in the supplied client sources:
 * - Legacy hh_entry: standard RC4-style stream cipher in #old mode
 * - hh_entry_init: the modified MUS cipher in #initMUS mode
 */
public class HabboCipher {

    private static final byte[] XOR_KEY =
            "mWxFRJnGJ5T9Si0OMVvEBBm8laihXkN8GmH6fuv7ldZhLyGRRKCcGzziPYBaJom"
                    .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PREMIX_STRING =
            "NV6VVFPoC7FLDlzDUri3qcOAg9cRoFOmsYR9ffDGy5P8HfF6eekX40SFSVfJ1mDb3lcpYRqdg28sp61eHkPukKbqTu1JsVEKiRavi04YtSzUsLXaYSa5BEGwg5G2OF"
                    .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] POST_ENCRYPT_EXTRA =
            "xllVGKnnQcW8aX4WefdKrBWTqiW5EwT"
                    .getBytes(StandardCharsets.US_ASCII);
    private static final int PREMIX_ROUNDS = 52;

    private final int[] sbox = new int[256];
    private Mode mode = Mode.LEGACY;
    private int q;
    private int j;

    public void initLegacy(byte[] sharedKey) {
        mode = Mode.LEGACY;
        initSbox(sharedKey);
    }

    public void initMus(byte[] sharedKey) {
        mode = Mode.MUS;

        byte[] modKey = new byte[sharedKey.length];
        for (int i = 0; i < sharedKey.length; i++) {
            modKey[i] = (byte) ((sharedKey[i] & 0xFF) ^ (XOR_KEY[i % XOR_KEY.length] & 0xFF));
        }

        initSbox(modKey);

        for (int round = 0; round < PREMIX_ROUNDS; round++) {
            runMusPrga(PREMIX_STRING);
        }
    }

    public HabboCipher copy() {
        HabboCipher copy = new HabboCipher();
        copy.mode = mode;
        copy.q = q;
        copy.j = j;
        System.arraycopy(sbox, 0, copy.sbox, 0, sbox.length);
        return copy;
    }

    /**
     * Decrypt a full client frame represented as ASCII hex on the wire.
     */
    public byte[] decryptFrame(byte[] hexData) {
        if ((hexData.length % 2) != 0) {
            throw new IllegalArgumentException("Encrypted frame length must be even");
        }

        byte[] encryptedBytes = decodeHex(hexData);
        byte[] plaintext = mode == Mode.MUS ? runMusPrga(encryptedBytes) : runLegacyPrga(encryptedBytes);

        if (mode == Mode.MUS) {
            runMusPrga(POST_ENCRYPT_EXTRA);
        }

        return plaintext;
    }

    private void initSbox(byte[] keyBytes) {
        int[] key = new int[256];
        for (int i = 0; i < 256; i++) {
            key[i] = keyBytes[i % keyBytes.length] & 0xFF;
            sbox[i] = i;
        }

        int swapIndex = 0;
        for (int i = 0; i < 256; i++) {
            swapIndex = (swapIndex + sbox[i] + key[i]) & 0xFF;
            swap(i, swapIndex);
        }

        q = 0;
        j = 0;
    }

    private byte[] runLegacyPrga(byte[] input) {
        byte[] output = new byte[input.length];

        for (int index = 0; index < input.length; index++) {
            q = (q + 1) & 0xFF;
            j = (j + sbox[q]) & 0xFF;
            swap(q, j);

            int keyByte = sbox[(sbox[q] + sbox[j]) & 0xFF];
            output[index] = (byte) ((input[index] & 0xFF) ^ keyByte);
        }

        return output;
    }

    private byte[] runMusPrga(byte[] input) {
        byte[] output = new byte[input.length];

        for (int index = 0; index < input.length; index++) {
            q = (q + 1) & 0xFF;
            j = (j + sbox[q]) & 0xFF;
            swap(q, j);

            int secondaryIndex = (17 * (q + 19)) & 0xFF;
            int secondarySwapIndex = (j + sbox[secondaryIndex]) & 0xFF;
            swap(secondaryIndex, secondarySwapIndex);

            if (q == 46 || q == 67 || q == 192) {
                int tertiaryIndex = (297 * (secondaryIndex + 67)) & 0xFF;
                int tertiarySwapIndex = (secondarySwapIndex + sbox[tertiaryIndex]) & 0xFF;
                swap(tertiaryIndex, tertiarySwapIndex);
            }

            int keyByte = sbox[(sbox[q] + sbox[j]) & 0xFF];
            output[index] = (byte) ((input[index] & 0xFF) ^ keyByte);
        }

        return output;
    }

    private byte[] decodeHex(byte[] hexData) {
        byte[] decoded = new byte[hexData.length / 2];

        for (int i = 0; i < decoded.length; i++) {
            int highNibble = hexCharToNibble(hexData[i * 2]);
            int lowNibble = hexCharToNibble(hexData[i * 2 + 1]);
            decoded[i] = (byte) ((highNibble << 4) | lowNibble);
        }

        return decoded;
    }

    private void swap(int left, int right) {
        int temp = sbox[left];
        sbox[left] = sbox[right];
        sbox[right] = temp;
    }

    private static int hexCharToNibble(byte value) {
        if (value >= '0' && value <= '9') {
            return value - '0';
        }
        if (value >= 'A' && value <= 'F') {
            return value - 'A' + 10;
        }
        if (value >= 'a' && value <= 'f') {
            return value - 'a' + 10;
        }
        throw new IllegalArgumentException("Invalid hex char: " + (char) value);
    }

    private enum Mode {
        LEGACY,
        MUS
    }
}
