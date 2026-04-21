package org.oldskooler.vibe.web.cms.auth;

import io.javalin.http.Context;
import org.oldskooler.vibe.web.cms.admin.CmsAdminDao;
import org.oldskooler.vibe.web.cms.admin.CmsAdminUser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public final class SignedSessionService {

    private static final String COOKIE_NAME = "vibe_web_session";
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 12;

    private final String sessionSecret;

    /**
     * Creates a new SignedSessionService.
     * @param sessionSecret the session secret value
     */
    public SignedSessionService(String sessionSecret) {
        this.sessionSecret = sessionSecret;
    }

    /**
     * Starts a signed admin session.
     * @param context the context value
     * @param adminUser the admin user value
     */
    public void start(Context context, CmsAdminUser adminUser) {
        long expiresAt = Instant.now().plusSeconds(COOKIE_MAX_AGE_SECONDS).getEpochSecond();
        String payload = adminUser.id() + "|" + expiresAt;
        String token = payload + "|" + sign(payload);
        context.res().addHeader("Set-Cookie", buildCookieHeader(token, COOKIE_MAX_AGE_SECONDS));
    }

    /**
     * Clears the current session.
     * @param context the context value
     */
    public void clear(Context context) {
        context.res().addHeader("Set-Cookie", buildCookieHeader("", 0));
    }

    /**
     * Authenticates the current request cookie.
     * @param context the context value
     * @return the resulting admin user
     */
    public Optional<CmsAdminUser> authenticate(Context context) {
        String token = context.cookie(COOKIE_NAME);
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\|");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String payload = parts[0] + "|" + parts[1];
        if (!constantTimeEquals(sign(payload), parts[2])) {
            return Optional.empty();
        }

        long expiresAt = Long.parseLong(parts[1]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            return Optional.empty();
        }

        int adminId = Integer.parseInt(parts[0]);
        return CmsAdminDao.findById(adminId);
    }

    /**
     * Signs a session payload.
     * @param payload the payload value
     * @return the resulting signature
     */
    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign cms session payload", e);
        }
    }

    /**
     * Builds a cookie header.
     * @param value the cookie value
     * @param maxAgeSeconds the max age in seconds
     * @return the resulting header value
     */
    private static String buildCookieHeader(String value, int maxAgeSeconds) {
        return COOKIE_NAME + "=" + value + "; Max-Age=" + maxAgeSeconds + "; Path=/; HttpOnly; SameSite=Lax";
    }

    /**
     * Compares strings in constant time.
     * @param left the left value
     * @param right the right value
     * @return the resulting state
     */
    private static boolean constantTimeEquals(String left, String right) {
        byte[] expected = left.getBytes(StandardCharsets.UTF_8);
        byte[] actual = right.getBytes(StandardCharsets.UTF_8);
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
