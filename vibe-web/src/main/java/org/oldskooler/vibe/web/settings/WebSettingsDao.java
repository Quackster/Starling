package org.oldskooler.vibe.web.settings;

import org.oldskooler.vibe.storage.EntityContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class WebSettingsDao {

    /**
     * Creates a new WebSettingsDao.
     */
    private WebSettingsDao() {}

    /**
     * Lists every stored setting in display order.
     * @return the resulting settings
     */
    public static List<WebSettingRecord> listAll() {
        return EntityContext.withContext(context -> context.from(WebSettingEntity.class)
                .orderBy(order -> order
                        .col(WebSettingEntity::getCategory).asc()
                        .col(WebSettingEntity::getSortOrder).asc()
                        .col(WebSettingEntity::getSettingKey).asc())
                .toList()
                .stream()
                .map(WebSettingsDao::map)
                .toList());
    }

    /**
     * Finds a setting by key.
     * @param key the setting key
     * @return the setting, when present
     */
    public static Optional<WebSettingRecord> findByKey(String key) {
        return EntityContext.withContext(context -> context.from(WebSettingEntity.class)
                .filter(filter -> filter.equals(WebSettingEntity::getSettingKey, key == null ? "" : key))
                .first()
                .map(WebSettingsDao::map));
    }

    /**
     * Inserts missing defaults and refreshes stored metadata for seeded settings.
     * @param definitions the definitions to seed
     */
    public static void seedMissing(List<WebSettingDefinition> definitions) {
        EntityContext.inTransaction(context -> {
            Timestamp now = Timestamp.from(Instant.now());
            for (WebSettingDefinition definition : definitions) {
                WebSettingEntity entity = context.from(WebSettingEntity.class)
                        .filter(filter -> filter.equals(WebSettingEntity::getSettingKey, definition.key()))
                        .first()
                        .orElse(null);
                if (entity == null) {
                    entity = new WebSettingEntity();
                    entity.setSettingKey(definition.key());
                    entity.setSettingValue(definition.normalizedDefaultValue());
                    entity.setCreatedAt(now);
                    applyMetadata(entity, definition);
                    entity.setUpdatedAt(now);
                    context.insert(entity);
                    continue;
                }

                applyMetadata(entity, definition);
                entity.setUpdatedAt(now);
                context.update(entity);
            }
            return null;
        });
    }

    /**
     * Updates a stored setting value.
     * @param definition the setting definition
     * @param value the new value
     */
    public static void updateValue(WebSettingDefinition definition, String value) {
        EntityContext.inTransaction(context -> {
            Timestamp now = Timestamp.from(Instant.now());
            WebSettingEntity entity = context.from(WebSettingEntity.class)
                    .filter(filter -> filter.equals(WebSettingEntity::getSettingKey, definition.key()))
                    .first()
                    .orElse(null);
            if (entity == null) {
                entity = new WebSettingEntity();
                entity.setSettingKey(definition.key());
                entity.setCreatedAt(now);
                applyMetadata(entity, definition);
                entity.setSettingValue(normalizeValue(value));
                entity.setUpdatedAt(now);
                context.insert(entity);
                return null;
            }

            applyMetadata(entity, definition);
            entity.setSettingValue(normalizeValue(value));
            entity.setUpdatedAt(now);
            context.update(entity);
            return null;
        });
    }

    private static void applyMetadata(WebSettingEntity entity, WebSettingDefinition definition) {
        entity.setCategory(definition.category());
        entity.setLabel(definition.label());
        entity.setDescription(definition.description());
        entity.setValueType(definition.valueType().name());
        entity.setIsSecret(definition.secret() ? 1 : 0);
        entity.setSortOrder(definition.sortOrder());
    }

    private static WebSettingRecord map(WebSettingEntity entity) {
        return new WebSettingRecord(
                entity.getSettingKey(),
                entity.getCategory(),
                entity.getLabel(),
                entity.getDescription(),
                WebSettingValueType.fromValue(entity.getValueType()),
                entity.getIsSecret() > 0,
                entity.getSortOrder(),
                entity.getSettingValue()
        );
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value;
    }
}
