package org.starling.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Diffie-Hellman key exchange helper for the legacy and initMUS login flows.
 */
public final class DiffieHellman {

    private static final BigInteger LEGACY_PRIME = new BigInteger(
            "455de99a7bcd4cf7a2d2ed03ad35ee047750cea4b446cd7e297102ebec1daaad", 16);
    private static final BigInteger LEGACY_GENERATOR = new BigInteger(
            "3ef9fba7796ba6145b4dac13739bb5604ee70e2dff95f9c5a846633a4e6e1a5b", 16);
    private static final BigInteger MUS_PRIME = new BigInteger(
            "A8EA077D4943CC98E53C21F5F7C7A0DB8BCE7506F8361A7C1690392F2B090C96" +
            "EE8BC67BAA0DCB7183F16401F5CB838E3B6EE86B9EF2E5D0F3C49D4DC4EDC2B9", 16);
    private static final BigInteger MUS_GENERATOR = BigInteger.valueOf(5L);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BigInteger prime;
    private final BigInteger privateKey;
    private final BigInteger publicKey;

    private DiffieHellman(BigInteger prime, BigInteger generator) {
        this.prime = prime;
        this.privateKey = new BigInteger(prime.bitLength() - 2, RANDOM);
        this.publicKey = generator.modPow(privateKey, prime);
    }

    public static DiffieHellman legacy() {
        return new DiffieHellman(LEGACY_PRIME, LEGACY_GENERATOR);
    }

    public static DiffieHellman mus() {
        return new DiffieHellman(MUS_PRIME, MUS_GENERATOR);
    }

    public String getPublicKeyHex() {
        return publicKey.toString(16);
    }

    public byte[] computeSharedSecret(String clientPublicKeyHex) {
        BigInteger clientPublic = new BigInteger(clientPublicKeyHex, 16);
        BigInteger sharedSecret = clientPublic.modPow(privateKey, prime);
        String sharedHex = sharedSecret.toString(16);

        if ((sharedHex.length() % 2) != 0) {
            sharedHex = "0" + sharedHex;
        }

        byte[] result = new byte[sharedHex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(sharedHex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}
