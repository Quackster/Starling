package org.starling.game.room.response.publicspace;

import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.PublicRoomItemDao;
import org.starling.storage.dao.RoomModelDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.PublicRoomItemEntity;
import org.starling.storage.entity.RoomModelEntity;

import java.util.List;

/**
 * Writes public-room furniture and wall-item packets from stored public-space data.
 */
public final class PublicRoomContentWriter {

    private final PublicRoomFurnitureSerializer legacyFurnitureSerializer = new PublicRoomFurnitureSerializer();
    private final PublicRoomItemSerializer itemSerializer = new PublicRoomItemSerializer();

    /**
     * Sends passive objects.
     * @param session the session value
     * @param roomId the room id value
     */
    public void sendPassiveObjects(Session session, int roomId) {
        PublicRoomEntity room = PublicRoomDao.findById(roomId);
        if (room == null) {
            sendEmptyObjects(session);
            return;
        }

        List<PublicRoomItemEntity> items = PublicRoomItemDao.findByRoomModel(room.getUnitStrId());
        if (!items.isEmpty()) {
            session.send(itemSerializer.buildObjectsMessage(items));
            session.send(itemSerializer.buildActiveObjectsMessage(items));
            return;
        }

        RoomModelEntity model = RoomModelDao.findByModelName(room.getUnitStrId(), true);
        if (model != null && !model.getPublicRoomItems().isBlank()) {
            session.send(legacyFurnitureSerializer.buildObjectsMessage(model.getPublicRoomItems()));
            session.send(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0));
            return;
        }

        sendEmptyObjects(session);
    }

    /**
     * Sends items.
     * @param session the session value
     * @param roomId the room id value
     */
    public void sendItems(Session session, int roomId) {
        PublicRoomEntity room = PublicRoomDao.findById(roomId);
        if (room == null) {
            session.send(new ServerMessage(OutgoingPackets.ROOM_ITEMS));
            return;
        }

        List<PublicRoomItemEntity> items = PublicRoomItemDao.findByRoomModel(room.getUnitStrId());
        if (!items.isEmpty()) {
            session.send(itemSerializer.buildItemsMessage(items));
            return;
        }

        session.send(new ServerMessage(OutgoingPackets.ROOM_ITEMS));
    }

    /**
     * Sends empty objects.
     * @param session the session value
     */
    private void sendEmptyObjects(Session session) {
        session.send(new ServerMessage(OutgoingPackets.ROOM_OBJECTS).writeInt(0));
        session.send(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0));
    }
}
