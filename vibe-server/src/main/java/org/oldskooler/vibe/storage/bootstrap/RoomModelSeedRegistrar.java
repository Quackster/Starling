package org.oldskooler.vibe.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.storage.bootstrap.seed.data.RoomModelSeedCatalog;
import org.oldskooler.vibe.storage.entity.RoomModelEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RoomModelSeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(RoomModelSeedRegistrar.class);
    private static final List<RoomModelSeedCatalog.RoomModelSeed> DEFAULT_ROOM_MODELS = RoomModelSeedCatalog.roomModels();

    /**
     * Seeds.
     * @param context the context value
     */
    @Override
    public void seed(DbContext context) {
        try {
            Map<String, RoomModelEntity> modelsByName = new LinkedHashMap<>();
            for (RoomModelEntity entity : context.from(RoomModelEntity.class).toList()) {
                modelsByName.put(entity.getModelName(), entity);
            }

            for (RoomModelSeedCatalog.RoomModelSeed model : DEFAULT_ROOM_MODELS) {
                RoomModelEntity entity = modelsByName.get(model.modelName());
                boolean isNew = entity == null;
                if (isNew) {
                    entity = new RoomModelEntity();
                    entity.setModelName(model.modelName());
                }

                entity.setIsPublic(model.isPublic());
                entity.setDoorX(model.doorX());
                entity.setDoorY(model.doorY());
                entity.setDoorZ(model.doorZ());
                entity.setDoorDir(model.doorDir());
                entity.setHeightmap(model.heightmap());
                entity.setPublicRoomItems(model.publicRoomItems());
                entity.setWallpaper(model.wallpaper());
                entity.setFloorPattern(model.floorPattern());
                entity.setLandscape(model.landscape());

                if (isNew) {
                    context.insert(entity);
                } else {
                    context.update(entity);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed room models", e);
        }

        log.info("Ensured default room models exist");
    }
}
