package org.starling.web.user;

import io.javalin.http.Context;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.web.settings.WebSettingsService;

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
    private static final String AUTH_STATE_ATTRIBUTE = "starling_user_session_state";
    private static final int SESSION_MAX_AGE_SECONDS = 60 * 60 * 12;
    private static final int REMEMBER_ME_MAX_AGE_SECONDS = 60 * 60 * 24 * 7;
    private static final int SESSION_TOKEN_BYTES = 32;
    private static final long ACTIVITY_REFRESH_INTERVAL_MILLIS = 60_000L;
    private static final long REAUTHENTICATION_GRACE_PERIOD_MILLIS = 5_000L;
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final String REAUTHENTICATE_PATH_SESSION_KEY = "reauthenticatePath";
    public static final String RECENT_REAUTHENTICATION_AT_SESSION_KEY = "recentReauthenticatedAt";

    private final String sessionSecret;
    private final WebSettingsService webSettingsService;

    /**
     * Creates a new UserSessionService.
     * @param sessionSecret the session secret value
     */
    public UserSessionService(String sessionSecret) {
        this.sessionSecret = sessionSecret;
        this.webSettingsService = null;
    }

    /**
     * Creates a new UserSessionService backed by persisted settings.
     * @param webSettingsService the current web settings
     */
    public UserSessionService(WebSettingsService webSettingsService) {
        this.sessionSecret = null;
        this.webSettingsService = webSettingsService;
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
        long now = System.currentTimeMillis();
        int maxAgeSeconds = rememberMe ? REMEMBER_ME_MAX_AGE_SECONDS : SESSION_MAX_AGE_SECONDS;
        long expiresAtMillis = now + (maxAgeSeconds * 1000L);
        String rawToken = newSessionToken();
        String tokenHash = hashToken(rawToken);
        String state = encodeState(expiresAtMillis, now, rememberMe);
        String payload = tokenHash + "|" + state;

        UserSessionTokenDao.storeToken(user.getId(), rawToken);
        context.res().addHeader(
                "Set-Cookie",
                buildCookieHeader(payload + "|" + sign(payload), rememberMe ? maxAgeSeconds : null)
        );
        context.attribute(AUTH_STATE_ATTRIBUTE, null);
        context.sessionAttribute(RECENT_REAUTHENTICATION_AT_SESSION_KEY, null);
    }

    /**
     * Restarts the current session after a successful reauthentication.
     * @param context the request context
     * @param user the user value
     */
    public void restartAfterReauthentication(Context context, UserEntity user) {
        boolean rememberMe = parseCookie(context.cookie(COOKIE_NAME), false)
                .map(UserSessionCookie::persistent)
                .orElse(false);
        start(context, user, rememberMe);
        context.sessionAttribute(RECENT_REAUTHENTICATION_AT_SESSION_KEY, System.currentTimeMillis());
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
        context.attribute(AUTH_STATE_ATTRIBUTE, null);
        context.sessionAttribute(RECENT_REAUTHENTICATION_AT_SESSION_KEY, null);
    }

    /**
     * Authenticates the current request cookie.
     * @param context the context value
     * @return the resulting user
     */
    public Optional<UserEntity> authenticate(Context context) {
        return authState(context).user();
    }

    /**
     * Returns whether the current session must reauthenticate before protected actions.
     * @param context the request context
     * @return true when password confirmation is required
     */
    public boolean isReauthenticationRequired(Context context) {
        Long recentReauthenticationAt = context.sessionAttribute(RECENT_REAUTHENTICATION_AT_SESSION_KEY);
        if (recentReauthenticationAt != null
                && System.currentTimeMillis() - recentReauthenticationAt <= REAUTHENTICATION_GRACE_PERIOD_MILLIS) {
            return false;
        }
        return authState(context).reauthenticationRequired();
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(currentSessionSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
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
            CookieState cookieState = decodeState(parts[1]);
            if (enforceExpiry && System.currentTimeMillis() > cookieState.expiresAtMillis()) {
                return Optional.empty();
            }

            String payload = parts[0] + "|" + parts[1];
            if (!constantTimeEquals(sign(payload), parts[2])) {
                return Optional.empty();
            }

            return Optional.of(new UserSessionCookie(
                    parts[0],
                    cookieState.expiresAtMillis(),
                    cookieState.lastActivityAtMillis(),
                    cookieState.persistent(),
                    parts[2]
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String newSessionToken() {
        byte[] tokenBytes = new byte[SESSION_TOKEN_BYTES];
        RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String currentSessionSecret() {
        return webSettingsService == null ? sessionSecret : webSettingsService.sessionSecret();
    }

    private AuthState authState(Context context) {
        AuthState cached = context.attribute(AUTH_STATE_ATTRIBUTE);
        if (cached != null) {
            return cached;
        }

        Optional<UserSessionCookie> cookie = parseCookie(context.cookie(COOKIE_NAME), true);
        if (cookie.isEmpty()) {
            AuthState state = new AuthState(Optional.empty(), Optional.empty(), false);
            context.attribute(AUTH_STATE_ATTRIBUTE, state);
            return state;
        }

        OptionalInt userId = UserSessionTokenDao.findUserIdByTokenHash(cookie.get().tokenHash());
        UserEntity user = userId.isPresent() ? UserDao.findById(userId.getAsInt()) : null;
        if (user == null) {
            AuthState state = new AuthState(Optional.empty(), cookie, false);
            context.attribute(AUTH_STATE_ATTRIBUTE, state);
            return state;
        }

        boolean reauthenticationRequired = requiresReauthentication(cookie.get());
        if (!reauthenticationRequired) {
            refreshActivityCookie(context, cookie.get());
        }

        AuthState state = new AuthState(Optional.of(user), cookie, reauthenticationRequired);
        context.attribute(AUTH_STATE_ATTRIBUTE, state);
        return state;
    }

    private boolean requiresReauthentication(UserSessionCookie cookie) {
        long lastActivityAtMillis = cookie.lastActivityAtMillis();
        if (lastActivityAtMillis <= 0) {
            return false;
        }

        long idleTimeoutMillis = Math.max(0, (long) reauthenticationIdleMinutes() * 60_000L);
        return System.currentTimeMillis() - lastActivityAtMillis > idleTimeoutMillis;
    }

    private void refreshActivityCookie(Context context, UserSessionCookie cookie) {
        long now = System.currentTimeMillis();
        if (cookie.lastActivityAtMillis() > 0 && now - cookie.lastActivityAtMillis() < ACTIVITY_REFRESH_INTERVAL_MILLIS) {
            return;
        }

        String state = encodeState(cookie.expiresAtMillis(), now, cookie.persistent());
        String payload = cookie.tokenHash() + "|" + state;
        context.res().addHeader(
                "Set-Cookie",
                buildCookieHeader(payload + "|" + sign(payload), cookie.persistent() ? remainingMaxAgeSeconds(cookie.expiresAtMillis(), now) : null)
        );
    }

    private CookieState decodeState(String encodedState) {
        if (encodedState == null || encodedState.isBlank()) {
            throw new IllegalArgumentException("Missing session state");
        }

        if (!encodedState.contains(":")) {
            long expiresAtMillis = Long.parseLong(encodedState) * 1000L;
            return new CookieState(expiresAtMillis, 0L, false);
        }

        String[] parts = encodedState.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid session state");
        }
        return new CookieState(
                Long.parseLong(parts[0]),
                Long.parseLong(parts[1]),
                "1".equals(parts[2]) || "true".equalsIgnoreCase(parts[2])
        );
    }

    private String encodeState(long expiresAtMillis, long lastActivityAtMillis, boolean persistent) {
        return expiresAtMillis + ":" + lastActivityAtMillis + ":" + (persistent ? "1" : "0");
    }

    private Integer remainingMaxAgeSeconds(long expiresAtMillis, long now) {
        long remainingMillis = Math.max(0L, expiresAtMillis - now);
        return Math.toIntExact((remainingMillis + 999L) / 1000L);
    }

    private int reauthenticationIdleMinutes() {
        return webSettingsService == null ? 30 : webSettingsService.reauthenticateIdleMinutes();
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

    private record CookieState(long expiresAtMillis, long lastActivityAtMillis, boolean persistent) {
    }

    private record AuthState(Optional<UserEntity> user, Optional<UserSessionCookie> cookie, boolean reauthenticationRequired) {
    }
}
