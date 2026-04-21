package org.oldskooler.vibe.game.room.response.publicspace;

import org.oldskooler.vibe.message.OutgoingPackets;
import org.oldskooler.vibe.net.codec.ServerMessage;
import org.oldskooler.vibe.storage.entity.PublicRoomItemEntity;

import java.util.List;
import java.util.Locale;

/**
 * Serializes structured public-room items into world, active, and wall-item packets.
 */
final class PublicRoomItemSerializer {

    /**
     * Builds objects message.
     * @param items the items value
     * @return the resulting build objects message
     */
    ServerMessage buildObjectsMessage(List<PublicRoomItemEntity> items) {
        List<PublicRoomItemEntity> worldItems = items.stream()
                .filter(this::isWorldObject)
                .toList();

        ServerMessage message = new ServerMessage(OutgoingPackets.ROOM_OBJECTS).writeInt(worldItems.size());
        for (PublicRoomItemEntity item : worldItems) {
            message.writeRaw(instanceId(item) + " ");
            message.writeString(item.getSprite());
            message.writeRaw(item.getX() + " " + item.getY() + " " + (int) item.getZ() + " " + item.getRotation());
            if (hasBehaviour(item, "extra_parameter")) {
                message.writeRaw(" 2");
            }
            message.writeRaw("\r");
        }
        return message;
    }

    /**
     * Builds active objects message.
     * @param items the items value
     * @return the resulting build active objects message
     */
    ServerMessage buildActiveObjectsMessage(List<PublicRoomItemEntity> items) {
        List<PublicRoomItemEntity> activeItems = items.stream()
                .filter(this::isActiveObject)
                .toList();

        ServerMessage message = new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(activeItems.size());
        for (PublicRoomItemEntity item : activeItems) {
            message.writeString(Integer.toString(item.getId()));
            message.writeString(item.getSprite());
            message.writeInt(item.getX());
            message.writeInt(item.getY());
            message.writeInt(Math.max(item.getLength(), 0));
            message.writeInt(Math.max(item.getWidth(), 0));
            message.writeInt(item.getRotation());
            message.writeString(formatHeight(item.getZ()));
            message.writeString("");
            message.writeString("");
            message.writeInt(0);
            message.writeString("");
        }
        return message;
    }

    /**
     * Builds items message.
     * @param items the items value
     * @return the resulting build items message
     */
    ServerMessage buildItemsMessage(List<PublicRoomItemEntity> items) {
        ServerMessage message = new ServerMessage(OutgoingPackets.ROOM_ITEMS);
        for (PublicRoomItemEntity item : items) {
            if (!hasBehaviour(item, "wall_item")) {
                continue;
            }

            message.writeRaw(item.getId() + "\t");
            message.writeRaw(item.getSprite() + "\t");
            message.writeRaw(" \t");
            message.writeRaw(":w=0,0 l=0,0 l\t");
            message.writeRaw(item.getCurrentProgram());
            message.writeRaw("\r");
        }
        return message;
    }

    /**
     * Ises world object.
     * @param item the item value
     * @return the result of this operation
     */
    private boolean isWorldObject(PublicRoomItemEntity item) {
        return !hasBehaviour(item, "private_furniture")
                && !hasBehaviour(item, "wall_item")
                && !isQueueTile(item)
                && !hasBehaviour(item, "invisible");
    }

    /**
     * Ises active object.
     * @param item the item value
     * @return the result of this operation
     */
    private boolean isActiveObject(PublicRoomItemEntity item) {
        return !hasBehaviour(item, "wall_item")
                && (hasBehaviour(item, "private_furniture") || isQueueTile(item));
    }

    /**
     * Ises queue tile.
     * @param item the item value
     * @return the result of this operation
     */
    private boolean isQueueTile(PublicRoomItemEntity item) {
        return item.getSprite().toLowerCase(Locale.ROOT).contains("queue_tile2");
    }

    /**
     * Hases behaviour.
     * @param item the item value
     * @param behaviour the behaviour value
     * @return the result of this operation
     */
    private boolean hasBehaviour(PublicRoomItemEntity item, String behaviour) {
        String raw = item.getBehaviour();
        if (raw.isBlank()) {
            return false;
        }

        for (String token : raw.split(",")) {
            if (behaviour.equalsIgnoreCase(token.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Instances id.
     * @param item the item value
     * @return the result of this operation
     */
    private String instanceId(PublicRoomItemEntity item) {
        return "pub" + Integer.toString(item.getId(), 36);
    }

    /**
     * Formats height.
     * @param value the value value
     * @return the resulting format height
     */
    private String formatHeight(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return Double.toString(value);
    }
}
