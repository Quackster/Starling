package org.starling.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

/** Diffie-Hellman key exchange helper for the hh_entry_init login flow. */
public final class DiffieHellman {

    private static final BigInteger INIT_PRIME = new BigInteger(
            "A8EA077D4943CC98E53C21F5F7C7A0DB8BCE7506F8361A7C1690392F2B090C96" +
            "EE8BC67BAA0DCB7183F16401F5CB838E3B6EE86B9EF2E5D0F3C49D4DC4EDC2B9", 16);
    private static final BigInteger INIT_GENERATOR = BigInteger.valueOf(5L);
    private static final String INIT_PRIVATE_CHARS = "012345679abcdef";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BigInteger prime;
    private final BigInteger privateKey;
    private final BigInteger publicKey;

    private DiffieHellman(BigInteger prime, BigInteger privateKey, BigInteger publicKey) {
        this.prime = prime;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public static DiffieHellman init() {
        return generate(INIT_PRIME, INIT_GENERATOR, 40, INIT_PRIVATE_CHARS, 72, 4);
    }

    public String getPublicKeyHex() {
        return publicKey.toString(16).toUpperCase();
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

    private static DiffieHellman generate(
            BigInteger prime,
            BigInteger generator,
            int privateHexBytes,
            String alphabet,
            int minimumPublicLength,
            int maxAttempts
    ) {
        BigInteger privateKey = BigInteger.ONE;
        BigInteger publicKey = BigInteger.ONE;
        String publicKeyHex = "1";

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String privateHex = randomHex(privateHexBytes * 2, alphabet);
            privateKey = new BigInteger(privateHex, 16);
            publicKey = generator.modPow(privateKey, prime);
            publicKeyHex = publicKey.toString(16);
            if (publicKeyHex.length() >= minimumPublicLength) {
                break;
            }
        }

        return new DiffieHellman(prime, privateKey, publicKey);
    }

    private static String randomHex(int length, String alphabet) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return value.toString();
    }
}
