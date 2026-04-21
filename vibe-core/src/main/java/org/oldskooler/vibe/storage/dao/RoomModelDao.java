package org.oldskooler.vibe.storage.dao;

import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.entity.RoomModelEntity;

public final class RoomModelDao {

    /**
     * Creates a new RoomModelDao.
     */
    private RoomModelDao() {}

    /**
     * Finds by model name.
     * @param modelName the model name value
     * @param publicModel the public model value
     * @return the resulting find by model name
     */
    public static RoomModelEntity findByModelName(String modelName, boolean publicModel) {
        if (modelName == null || modelName.isBlank() || !EntityContext.isInitialized()) {
            return null;
        }

        return EntityContext.withContext(context -> context.from(RoomModelEntity.class)
                .filter(filter -> filter
                        .equalsIgnoreCase(RoomModelEntity::getModelName, modelName.trim())
                        .equals(RoomModelEntity::getIsPublic, publicModel ? 1 : 0))
                .limit(1)
                .first()
                .orElse(null));
    }
}
