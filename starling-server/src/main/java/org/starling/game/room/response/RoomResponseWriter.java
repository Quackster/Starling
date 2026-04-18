package org.starling.game.room.response;

import org.starling.contracts.PublicRoomItem;
import org.starling.contracts.RoomOccupant;
import org.starling.contracts.RoomSnapshot;
import org.starling.gateway.LocalRoomSnapshotMapper;
import org.starling.game.player.Player;
import org.starling.message.OutgoingPackets;
import org.starling.message.support.HandlerResponses;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Sends room bootstrap and room-state packets from room service snapshots.
 */
public final class RoomResponseWriter {

    private static final String HOLOGRAPH_ROOM_URL = "http://wwww.vista4life.com/bf.php?p=emu";

    /**
     * Sends interstitial.
     * @param session the session value
     */
    public void sendInterstitial(Session session) {
        session.send(HandlerResponses.singleZeroMessage(OutgoingPackets.INTERSTITIAL_DATA));
    }

    /**
     * Sends private room directory.
     * @param session the session value
     */
    public void sendPrivateRoomDirectory(Session session) {
        session.send(new ServerMessage(OutgoingPackets.OPC_OK));
        sendRoomUrl(session);
    }

    /**
     * Enters public room.
     * @param session the session value
     * @param snapshot the snapshot value
     */
    public void enterPublicRoom(Session session, RoomSnapshot snapshot) {
        session.send(new ServerMessage(OutgoingPackets.OPC_OK));
        sendRoomUrl(session);
        session.send(buildRoomReadyMessage(snapshot));
    }

    /**
     * Allows private room entry.
     * @param session the session value
     */
    public void allowPrivateRoomEntry(Session session) {
        session.send(new ServerMessage(OutgoingPackets.FLAT_LETIN));
    }

    /**
     * Enters private room.
     * @param session the session value
     * @param player the player value
     * @param snapshot the snapshot value
     */
    public void enterPrivateRoom(Session session, Player player, RoomSnapshot snapshot) {
        session.send(buildRoomReadyMessage(snapshot));
        sendPrivateRoomProperties(session, snapshot);
        sendRoomRights(session, snapshot);
    }

    /**
     * Sends heightmap.
     * @param session the session value
     * @param snapshot the snapshot value
     */
    public void sendHeightmap(Session session, RoomSnapshot snapshot) {
        session.send(new ServerMessage(OutgoingPackets.HEIGHTMAP).writeRaw(snapshot.getVisuals().getHeightmap()));
    }

    /**
     * Sends users.
     * @param session the session value
     * @param snapshot the snapshot value
     */
    public void sendUsers(Session session, RoomSnapshot snapshot) {
        session.send(new ServerMessage(OutgoingPackets.ROOM_USERS)
                .writeRaw(buildUserObjectsPayload(snapshot.getOccupantsList())));
    }

    /**
     * Sends passive objects.
     * @param session the session value
     * @param snapshot the snapshot value
     */
    public void sendPassiveObjects(Session session, RoomSnapshot snapshot) {
        if (snapshot.getRoomType() != org.starling.contracts.RoomType.ROOM_TYPE_PUBLIC) {
            session.send(new ServerMessage(OutgoingPackets.ROOM_OBJECTS).writeInt(0));
            session.send(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0));
            return;
        }

        List<PublicRoomItem> items = snapshot.getPublicRoomItemsList();
        if (!items.isEmpty()) {
            session.send(buildObjectsMessage(items));
            session.send(buildActiveObjectsMessage(items));
            return;
        }

        if (!snapshot.getLegacyPublicRoomItems().isBlank()) {
            session.send(buildLegacyObjectsMessage(snapshot.getLegacyPublicRoomItems()));
            session.send(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0));
            return;
        }

        session.send(new ServerMessage(OutgoingPackets.ROOM_OBJECTS).writeInt(0));
        session.send(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0));
    }

    /**
     * Sends items.
     * @param session the session value
     * @param snapshot the snapshot value
     */
    public void sendItems(Session session, RoomSnapshot snapshot) {
        if (snapshot.getRoomType() != org.starling.contracts.RoomType.ROOM_TYPE_PUBLIC) {
            session.send(new ServerMessage(OutgoingPackets.ROOM_ITEMS));
            return;
        }

        ServerMessage message = new ServerMessage(OutgoingPackets.ROOM_ITEMS);
        for (PublicRoomItem item : snapshot.getPublicRoomItemsList()) {
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
        session.send(message);
    }

    /**
     * Sends status.
     * @param session the session value
     * @param snapshot the snapshot value
     */
    public void sendStatus(Session session, RoomSnapshot snapshot) {
        session.send(new ServerMessage(OutgoingPackets.STATUS).writeRaw(buildUserStatusPayload(snapshot.getOccupantsList())));
    }

    /**
     * Broadcasts status.
     * @param sessions the sessions value
     * @param snapshot the snapshot value
     */
    public void broadcastStatus(Collection<Session> sessions, RoomSnapshot snapshot) {
        ServerMessage message = new ServerMessage(OutgoingPackets.STATUS)
                .writeRaw(buildUserStatusPayload(snapshot.getOccupantsList()));
        for (Session occupant : sessions) {
            occupant.send(message);
        }
    }

    /**
     * Builds a status packet from a loaded room for leftover in-process callers
     * and test doubles.
     * @param room the room value
     */
    public void broadcastStatus(org.starling.game.room.registry.LoadedRoom<?> room) {
        if (room == null || room.isEmpty()) {
            return;
        }

        Session anchor = room.getSessions().stream().findFirst().orElse(null);
        if (anchor == null) {
            return;
        }

        broadcastStatus(room.getSessions(), LocalRoomSnapshotMapper.toRoomSnapshot(anchor, room));
    }

    /**
     * Sends room ad.
     * @param session the session value
     */
    public void sendRoomAd(Session session) {
        session.send(HandlerResponses.singleZeroMessage(OutgoingPackets.ROOM_AD));
    }

    /**
     * Sends spectator amount.
     * @param session the session value
     */
    public void sendSpectatorAmount(Session session) {
        session.send(new ServerMessage(OutgoingPackets.SPECTATOR_AMOUNT)
                .writeInt(0)
                .writeInt(0));
    }

    /**
     * Sends hotel view.
     * @param session the session value
     */
    public void sendHotelView(Session session) {
        session.send(new ServerMessage(OutgoingPackets.HOTEL_VIEW));
    }

    /**
     * Sends logout.
     * @param session the session value
     * @param playerId the player id value
     */
    public void sendLogout(Session session, int playerId) {
        session.send(new ServerMessage(OutgoingPackets.LOGOUT).writeInt(playerId));
    }

    /**
     * Sends room url.
     * @param session the session value
     */
    private void sendRoomUrl(Session session) {
        session.send(new ServerMessage(OutgoingPackets.ROOM_URL).writeRaw(HOLOGRAPH_ROOM_URL));
    }

    /**
     * Builds room ready message.
     * @param snapshot the snapshot value
     * @return the resulting build room ready message
     */
    private ServerMessage buildRoomReadyMessage(RoomSnapshot snapshot) {
        return new ServerMessage(OutgoingPackets.ROOM_READY)
                .writeRaw(snapshot.getVisuals().getMarker() + " " + snapshot.getRoomId());
    }

    /**
     * Sends private room properties.
     * @param session the session value
     * @param snapshot the snapshot value
     */
    private void sendPrivateRoomProperties(Session session, RoomSnapshot snapshot) {
        sendFlatProperty(session, "landscape", snapshot.getVisuals().getLandscape());
        sendFlatProperty(session, "wallpaper", snapshot.getVisuals().getWallpaper());
        sendFlatProperty(session, "floor", snapshot.getVisuals().getFloorPattern());
    }

    /**
     * Sends flat property.
     * @param session the session value
     * @param key the key value
     * @param value the value value
     */
    private void sendFlatProperty(Session session, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        session.send(new ServerMessage(OutgoingPackets.FLAT_PROPERTY).writeRaw(key + "/" + value));
    }

    /**
     * Sends room rights.
     * @param session the session value
     * @param snapshot the snapshot value
     */
    private void sendRoomRights(Session session, RoomSnapshot snapshot) {
        if (snapshot.getOwner()) {
            session.send(new ServerMessage(OutgoingPackets.ROOM_RIGHTS_OWNER));
        }
        if (snapshot.getController()) {
            session.send(new ServerMessage(OutgoingPackets.ROOM_RIGHTS_CONTROLLER));
        }
    }

    /**
     * Builds user objects payload.
     * @param occupants the occupants value
     * @return the resulting build user objects payload
     */
    private String buildUserObjectsPayload(List<RoomOccupant> occupants) {
        StringBuilder payload = new StringBuilder("\r");
        for (RoomOccupant occupant : occupants) {
            payload.append("i:").append(occupant.getPlayerId()).append('\r');
            payload.append("a:").append(occupant.getPlayerId()).append('\r');
            payload.append("n:").append(occupant.getUsername()).append('\r');
            payload.append("f:").append(occupant.getFigure()).append('\r');
            payload.append("s:").append(occupant.getSex()).append('\r');
            payload.append("l:").append(occupant.getX()).append(' ')
                    .append(occupant.getY()).append(' ')
                    .append(formatHeight(occupant.getZ())).append('\r');
            if (!occupant.getMotto().isBlank()) {
                payload.append("c:").append(occupant.getMotto()).append('\r');
            }
        }
        return payload.toString();
    }

    /**
     * Builds user status payload.
     * @param occupants the occupants value
     * @return the resulting build user status payload
     */
    private String buildUserStatusPayload(List<RoomOccupant> occupants) {
        StringBuilder payload = new StringBuilder();
        for (RoomOccupant occupant : occupants) {
            payload.append(occupant.getPlayerId()).append(' ')
                    .append(occupant.getX()).append(',')
                    .append(occupant.getY()).append(',')
                    .append(formatHeight(occupant.getZ())).append(',')
                    .append(occupant.getBodyRotation()).append(',')
                    .append(occupant.getHeadRotation()).append('/');
            if (occupant.getHasNextPosition()) {
                payload.append("mv ")
                        .append(occupant.getNextX()).append(',')
                        .append(occupant.getNextY()).append(',')
                        .append(formatHeight(occupant.getNextZ())).append('/');
            }
            payload.append('\r');
        }
        return payload.toString();
    }

    /**
     * Builds world object message.
     * @param items the items value
     * @return the resulting build objects message
     */
    private ServerMessage buildObjectsMessage(List<PublicRoomItem> items) {
        List<PublicRoomItem> worldItems = items.stream()
                .filter(this::isWorldObject)
                .toList();
        ServerMessage message = new ServerMessage(OutgoingPackets.ROOM_OBJECTS).writeInt(worldItems.size());
        for (PublicRoomItem item : worldItems) {
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
    private ServerMessage buildActiveObjectsMessage(List<PublicRoomItem> items) {
        List<PublicRoomItem> activeItems = items.stream()
                .filter(this::isActiveObject)
                .toList();
        ServerMessage message = new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(activeItems.size());
        for (PublicRoomItem item : activeItems) {
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
     * Builds legacy objects message.
     * @param rawFurniture the raw furniture value
     * @return the resulting build legacy objects message
     */
    private ServerMessage buildLegacyObjectsMessage(String rawFurniture) {
        ServerMessage message = new ServerMessage(OutgoingPackets.ROOM_OBJECTS);
        String normalized = rawFurniture == null ? "" : rawFurniture.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");
        List<String[]> parsed = java.util.Arrays.stream(lines)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> line.split("\\s+"))
                .filter(parts -> parts.length >= 6)
                .toList();
        message.writeInt(parsed.size());
        for (String[] parts : parsed) {
            message.writeRaw(parts[0] + " ");
            message.writeString(parts[1]);
            message.writeRaw(parts[2] + " " + parts[3] + " " + parts[4] + " " + parts[5]);
            if (parts.length > 6) {
                message.writeRaw(" " + String.join(" ", java.util.Arrays.copyOfRange(parts, 6, parts.length)));
            }
            message.writeRaw("\r");
        }
        return message;
    }

    /**
     * Returns whether world object.
     * @param item the item value
     * @return the result of this operation
     */
    private boolean isWorldObject(PublicRoomItem item) {
        return !hasBehaviour(item, "private_furniture")
                && !hasBehaviour(item, "wall_item")
                && !isQueueTile(item)
                && !hasBehaviour(item, "invisible");
    }

    /**
     * Returns whether active object.
     * @param item the item value
     * @return the result of this operation
     */
    private boolean isActiveObject(PublicRoomItem item) {
        return !hasBehaviour(item, "wall_item")
                && (hasBehaviour(item, "private_furniture") || isQueueTile(item));
    }

    /**
     * Returns whether queue tile.
     * @param item the item value
     * @return the result of this operation
     */
    private boolean isQueueTile(PublicRoomItem item) {
        return item.getSprite().toLowerCase(Locale.ROOT).contains("queue_tile2");
    }

    /**
     * Returns whether behaviour exists.
     * @param item the item value
     * @param behaviour the behaviour value
     * @return the result of this operation
     */
    private boolean hasBehaviour(PublicRoomItem item, String behaviour) {
        if (item.getBehaviour().isBlank()) {
            return false;
        }
        for (String token : item.getBehaviour().split(",")) {
            if (behaviour.equalsIgnoreCase(token.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns legacy instance id.
     * @param item the item value
     * @return the result of this operation
     */
    private String instanceId(PublicRoomItem item) {
        return "pub" + Integer.toString(item.getId(), 36);
    }

    /**
     * Formats height.
     * @param value the value value
     * @return the resulting format height
     */
    private String formatHeight(double value) {
        return Math.floor(value) == value ? Integer.toString((int) value) : Double.toString(value);
    }
}
