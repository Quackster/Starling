package org.starling.web.user;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.UserEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalInt;

public final class UserSessionTokenDao {

    /**
     * Creates a new UserSessionTokenDao.
     */
    private UserSessionTokenDao() {}

    /**
     * Stores a new public session token for the user.
     * @param userId the user id value
     * @param token the raw token value
     */
    public static void storeToken(int userId, String token) {
        EntityContext.inTransaction(context -> {
            UserEntity user = context.from(UserEntity.class)
                    .filter(filter -> filter.equals(UserEntity::getId, userId))
                    .first()
                    .orElse(null);
            if (user == null) {
                return null;
            }

            user.setRememberToken(token);
            user.setUpdatedAt(Timestamp.from(Instant.now()));
            context.update(user);
            return null;
        });
    }

    /**
     * Finds a user id by the cookie token hash.
     * @param tokenHash the token hash value
     * @return the resulting user id, when present
     */
    public static OptionalInt findUserIdByTokenHash(String tokenHash) {
        return EntityContext.withContext(context -> {
            return context.from(UserEntity.class)
                    .filter(filter -> filter.isNotNull(UserEntity::getRememberToken))
                    .toList()
                    .stream()
                    .filter(user -> hashesEqual(user.getRememberToken(), tokenHash))
                    .mapToInt(UserEntity::getId)
                    .findFirst();
        });
    }

    /**
     * Clears a stored token by cookie token hash.
     * @param tokenHash the token hash value
     */
    public static void clearTokenByHash(String tokenHash) {
        EntityContext.inTransaction(context -> {
            UserEntity user = context.from(UserEntity.class)
                    .filter(filter -> filter.isNotNull(UserEntity::getRememberToken))
                    .toList()
                    .stream()
                    .filter(candidate -> hashesEqual(candidate.getRememberToken(), tokenHash))
                    .findFirst()
                    .orElse(null);
            if (user == null) {
                return null;
            }

            user.setRememberToken(null);
            user.setUpdatedAt(Timestamp.from(Instant.now()));
            context.update(user);
            return null;
        });
    }

    private static boolean hashesEqual(String rawToken, String tokenHash) {
        if (rawToken == null || tokenHash == null) {
            return false;
        }
        return hashToken(rawToken).equals(tokenHash.toLowerCase(Locale.ROOT));
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
}
