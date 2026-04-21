package org.oldskooler.vibe.storage.dao;

import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.entity.PublicTagEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public final class PublicTagDao {

    private PublicTagDao() {}

    public static long count() {
        return EntityContext.withContext(context -> context.from(PublicTagEntity.class).count());
    }

    public static List<PublicTagEntity> listAll() {
        return EntityContext.withContext(context -> context.from(PublicTagEntity.class)
                .orderBy(order -> order
                        .col(PublicTagEntity::getType).asc()
                        .col(PublicTagEntity::getOwnerId).asc()
                        .col(PublicTagEntity::getId).asc())
                .toList());
    }

    public static List<PublicTagEntity> listByOwner(String type, int ownerId) {
        String normalizedType = normalizeType(type);
        return EntityContext.withContext(context -> context.from(PublicTagEntity.class)
                .filter(filter -> filter
                        .equals(PublicTagEntity::getOwnerId, ownerId)
                        .equalsIgnoreCase(PublicTagEntity::getType, normalizedType))
                .orderBy(order -> order.col(PublicTagEntity::getId).asc())
                .toList());
    }

    public static PublicTagEntity findByOwnerAndTag(String type, int ownerId, String tag) {
        String normalizedType = normalizeType(type);
        String normalizedTag = normalizeTag(tag);
        return EntityContext.withContext(context -> context.from(PublicTagEntity.class)
                .filter(filter -> filter
                        .equals(PublicTagEntity::getOwnerId, ownerId)
                        .equalsIgnoreCase(PublicTagEntity::getType, normalizedType)
                        .equalsIgnoreCase(PublicTagEntity::getTag, normalizedTag))
                .first()
                .orElse(null));
    }

    public static PublicTagEntity addTag(String type, int ownerId, String tag) {
        PublicTagEntity existing = findByOwnerAndTag(type, ownerId, tag);
        if (existing != null) {
            return existing;
        }

        PublicTagEntity entity = new PublicTagEntity();
        entity.setOwnerId(ownerId);
        entity.setType(normalizeType(type));
        entity.setTag(normalizeTag(tag));
        entity.setCreatedAt(Timestamp.from(Instant.now()));

        return EntityContext.inTransaction(context -> {
            context.insert(entity);
            return entity;
        });
    }

    public static void removeTag(String type, int ownerId, String tag) {
        String normalizedType = normalizeType(type);
        String normalizedTag = normalizeTag(tag);
        EntityContext.inTransaction(context -> {
            context.from(PublicTagEntity.class)
                    .filter(filter -> filter
                            .equals(PublicTagEntity::getOwnerId, ownerId)
                            .equalsIgnoreCase(PublicTagEntity::getType, normalizedType)
                            .equalsIgnoreCase(PublicTagEntity::getTag, normalizedTag))
                    .delete();
            return null;
        });
    }

    private static String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "user" : normalized;
    }

    private static String normalizeTag(String tag) {
        return tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
    }
}
