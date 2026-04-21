package org.oldskooler.vibe.web.cms.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashAndVerifyRoundTrip() {
        String hash = PasswordHasher.hash("very-secret");

        assertTrue(PasswordHasher.verify("very-secret", hash));
        assertFalse(PasswordHasher.verify("wrong-password", hash));
    }

    @Test
    void hashingUsesRandomSalt() {
        String first = PasswordHasher.hash("repeatable");
        String second = PasswordHasher.hash("repeatable");

        assertNotEquals(first, second);
    }
}
