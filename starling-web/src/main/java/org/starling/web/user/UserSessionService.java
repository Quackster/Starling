package org.starling.web.user;

import io.javalin.http.Context;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.OptionalInt;

public final class UserSessionService {

    private static final String COOKIE_NAME = "starling_user_session";
    private static final int SESSION_MAX_AGE_SECONDS = 60 * 60 * 12;
    private static final int REMEMBER_ME_MAX_AGE_SECONDS = 60 * 60 * 24 * 7;
    private static final int SESSION_TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String sessionSecret;

    /**
     * Creates a new UserSessionService.
     * @param sessionSecret the session secret value
     */
    public UserSessionService(String sessionSecret) {
        this.sessionSecret = sessionSecret;
    }

    /**
     * Starts a signed user session.
     * @param context the context value
     * @param user the user value
     */
    public void start(Context context, UserEntity user) {
        start(context, user, true);
    }

    /**
     * Starts a signed user session.
     * @param context the context value
     * @param user the user value
     * @param rememberMe whether the cookie should be persistent
     */
    public void start(Context context, UserEntity user, boolean rememberMe) {
        int maxAgeSeconds = rememberMe ? REMEMBER_ME_MAX_AGE_SECONDS : SESSION_MAX_AGE_SECONDS;
        long expiresAt = Instant.now().plusSeconds(maxAgeSeconds).getEpochSecond();
        String rawToken = newSessionToken();
        String tokenHash = hashToken(rawToken);
        String payload = tokenHash + "|" + expiresAt;

        UserSessionTokenDao.storeToken(user.getId(), rawToken);
        context.res().addHeader(
                "Set-Cookie",
                buildCookieHeader(payload + "|" + sign(payload), rememberMe ? maxAgeSeconds : null)
        );
    }

    /**
     * Clears the current session.
     * @param context the context value
     */
    public void clear(Context context) {
        parseCookie(context.cookie(COOKIE_NAME), false)
                .map(UserSessionCookie::tokenHash)
                .ifPresent(UserSessionTokenDao::clearTokenByHash);
        context.res().addHeader("Set-Cookie", buildCookieHeader("", 0));
    }

    /**
     * Authenticates the current request cookie.
     * @param context the context value
     * @return the resulting user
     */
    public Optional<UserEntity> authenticate(Context context) {
        Optional<UserSessionCookie> cookie = parseCookie(context.cookie(COOKIE_NAME), true);
        if (cookie.isEmpty()) {
            return Optional.empty();
        }

        OptionalInt userId = UserSessionTokenDao.findUserIdByTokenHash(cookie.get().tokenHash());
        return userId.isPresent()
                ? Optional.ofNullable(UserDao.findById(userId.getAsInt()))
                : Optional.empty();
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign public user session payload", e);
        }
    }

    private Optional<UserSessionCookie> parseCookie(String token, boolean enforceExpiry) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\|");
        if (parts.length != 3 || !parts[0].matches("[0-9a-f]{64}")) {
            return Optional.empty();
        }

        try {
            long expiresAt = Long.parseLong(parts[1]);
            if (enforceExpiry && Instant.now().getEpochSecond() > expiresAt) {
                return Optional.empty();
            }

            String payload = parts[0] + "|" + parts[1];
            if (!constantTimeEquals(sign(payload), parts[2])) {
                return Optional.empty();
            }

            return Optional.of(new UserSessionCookie(parts[0], expiresAt, parts[2]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String newSessionToken() {
        byte[] tokenBytes = new byte[SESSION_TOKEN_BYTES];
        RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hash.append(Character.forDigit((value >>> 4) & 0x0F, 16));
                hash.append(Character.forDigit(value & 0x0F, 16));
            }
            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash public user session token", e);
        }
    }

    private static String buildCookieHeader(String value, Integer maxAgeSeconds) {
        StringBuilder header = new StringBuilder(COOKIE_NAME)
                .append('=')
                .append(value)
                .append("; Path=/; HttpOnly; SameSite=Lax");
        if (maxAgeSeconds != null) {
            header.append("; Max-Age=").append(maxAgeSeconds);
        }
        return header.toString();
    }

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
