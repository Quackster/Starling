package org.starling.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;

import java.sql.PreparedStatement;
import java.util.List;

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
        String sql = """
                INSERT INTO public_room_items (
                    id,
                    room_model,
                    sprite,
                    x,
                    y,
                    z,
                    rotation,
                    top_height,
                    length,
                    width,
                    behaviour,
                    current_program,
                    teleport_to,
                    swim_to
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    room_model = VALUES(room_model),
                    sprite = VALUES(sprite),
                    x = VALUES(x),
                    y = VALUES(y),
                    z = VALUES(z),
                    rotation = VALUES(rotation),
                    top_height = VALUES(top_height),
                    length = VALUES(length),
                    width = VALUES(width),
                    behaviour = VALUES(behaviour),
                    current_program = VALUES(current_program),
                    teleport_to = VALUES(teleport_to),
                    swim_to = VALUES(swim_to)
                """;

        try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
            for (LisbonPublicItemCatalog.PublicRoomItemSeed item : DEFAULT_PUBLIC_ROOM_ITEMS) {
                statement.setInt(1, item.id());
                statement.setString(2, item.roomModel());
                statement.setString(3, item.sprite());
                statement.setInt(4, item.x());
                statement.setInt(5, item.y());
                statement.setDouble(6, item.z());
                statement.setInt(7, item.rotation());
                statement.setDouble(8, item.topHeight());
                statement.setInt(9, item.length());
                statement.setInt(10, item.width());
                statement.setString(11, item.behaviour());
                statement.setString(12, item.currentProgram());
                statement.setString(13, item.teleportTo());
                statement.setString(14, item.swimTo());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed public room items", e);
        }

        log.info("Ensured Lisbon public room items exist");
    }
}
