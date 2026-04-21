package org.oldskooler.vibe.web.cms.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;

    /**
     * Creates a new PasswordHasher.
     */
    private PasswordHasher() {}

    /**
     * Hashes a plaintext password.
     * @param plaintext the plaintext value
     * @return the resulting hash
     */
    public static String hash(String plaintext) {
        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            byte[] hash = derive(plaintext.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return ITERATIONS + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash cms password", e);
        }
    }

    /**
     * Verifies a plaintext password against the stored hash.
     * @param plaintext the plaintext value
     * @param storedHash the stored hash value
     * @return the resulting verification state
     */
    public static boolean verify(String plaintext, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getUrlDecoder().decode(parts[1]);
            byte[] expected = Base64.getUrlDecoder().decode(parts[2]);
            byte[] actual = derive(plaintext.toCharArray(), salt, iterations, expected.length * 8);
            return constantTimeEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Derives a password hash.
     * @param password the password chars
     * @param salt the salt bytes
     * @param iterations the iteration count
     * @param keyLength the key length
     * @return the resulting bytes
     * @throws Exception if the derivation fails
     */
    private static byte[] derive(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    /**
     * Compares byte arrays in constant time.
     * @param expected the expected value
     * @param actual the actual value
     * @return the resulting state
     */
    private static boolean constantTimeEquals(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            return false;
        }

        int diff = 0;
        for (int index = 0; index < expected.length; index++) {
            diff |= expected[index] ^ actual[index];
        }
        return diff == 0;
    }
}
