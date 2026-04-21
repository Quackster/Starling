package org.oldskooler.vibe.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.storage.entity.PublicRoomItemEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PublicRoomItemSeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(PublicRoomItemSeedRegistrar.class);
    private static final List<LisbonPublicItemCatalog.PublicRoomItemSeed> DEFAULT_PUBLIC_ROOM_ITEMS =
            LisbonPublicItemCatalog.load().publicRoomItems();

    /**
     * Seeds.
     * @param context the context value
     */
    @Override
    public void seed(DbContext context) {
        try {
            Map<Integer, PublicRoomItemEntity> itemsById = new HashMap<>();
            for (PublicRoomItemEntity entity : context.from(PublicRoomItemEntity.class).toList()) {
                itemsById.put(entity.getId(), entity);
            }

            for (LisbonPublicItemCatalog.PublicRoomItemSeed item : DEFAULT_PUBLIC_ROOM_ITEMS) {
                PublicRoomItemEntity entity = itemsById.get(item.id());
                boolean isNew = entity == null;
                if (isNew) {
                    entity = new PublicRoomItemEntity();
                    entity.setId(item.id());
                }

                entity.setRoomModel(item.roomModel());
                entity.setSprite(item.sprite());
                entity.setX(item.x());
                entity.setY(item.y());
                entity.setZ(item.z());
                entity.setRotation(item.rotation());
                entity.setTopHeight(item.topHeight());
                entity.setLength(item.length());
                entity.setWidth(item.width());
                entity.setBehaviour(item.behaviour());
                entity.setCurrentProgram(item.currentProgram());
                entity.setTeleportTo(item.teleportTo());
                entity.setSwimTo(item.swimTo());

                if (isNew) {
                    context.insert(entity);
                } else {
                    context.update(entity);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed public room items", e);
        }

        log.info("Ensured Lisbon public room items exist");
    }
}
