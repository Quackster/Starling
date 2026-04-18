package org.starling.gateway;

import org.starling.contracts.PlayerData;
import org.starling.contracts.RoomSnapshot;
import org.starling.contracts.RoomType;
import org.starling.game.player.Player;
import org.starling.game.player.PlayerManager;
import org.starling.net.session.Session;

import java.util.Collection;

/**
 * Gateway-side mappers between local session/player state and service contracts.
 */
public final class GatewayMappings {

    /**
     * Creates a new GatewayMappings.
     */
    private GatewayMappings() {}

    /**
     * Converts player to contract player data.
     * @param player the player value
     * @return the result of this operation
     */
    public static PlayerData toPlayerData(Player player) {
        if (player == null) {
            return PlayerData.getDefaultInstance();
        }

        return PlayerData.newBuilder()
                .setId(player.getId())
                .setUsername(player.getUsername())
                .setFigure(player.getFigure())
                .setSex(player.getSex())
                .setMotto(player.getMotto())
                .setRank(player.getRank())
                .setSelectedRoomId(player.getSelectedRoomId())
                .setHomeRoom(player.getHomeRoom())
                .setCredits(player.getCredits())
                .build();
    }

    /**
     * Updates gateway room presence from the room service snapshot.
     * @param session the session value
     * @param snapshot the snapshot value
     */
    public static void applyRoomSnapshot(Session session, RoomSnapshot snapshot) {
        if (session == null || snapshot == null) {
            return;
        }

        if (snapshot.getRoomType() == RoomType.ROOM_TYPE_PUBLIC) {
            session.setRoomPresence(Session.RoomPresence.activePublic(
                    snapshot.getRoomId(),
                    snapshot.getVisuals().getMarker(),
                    snapshot.getDoorId()
            ));
            return;
        }

        session.setRoomPresence(Session.RoomPresence.activePrivate(
                snapshot.getRoomId(),
                snapshot.getVisuals().getMarker()
        ));
    }

    /**
     * Returns currently connected sessions in a specific active room.
     * @param roomType the room type value
     * @param roomId the room id value
     * @return the result of this operation
     */
    public static Collection<Session> sessionsInRoom(RoomType roomType, int roomId) {
        Session.RoomType gatewayRoomType = roomType == RoomType.ROOM_TYPE_PUBLIC
                ? Session.RoomType.PUBLIC
                : Session.RoomType.PRIVATE;
        return PlayerManager.getInstance().getOnlineSessions().stream()
                .filter(session -> session.getRoomPresence().active())
                .filter(session -> session.getRoomPresence().type() == gatewayRoomType)
                .filter(session -> session.getRoomPresence().roomId() == roomId)
                .toList();
    }
}
