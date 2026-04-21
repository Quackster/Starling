package org.oldskooler.vibe.game.room.response.occupant;

import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.game.room.geometry.RoomPosition;
import org.oldskooler.vibe.game.room.layout.RoomLayoutRegistry;
import org.oldskooler.vibe.game.room.registry.LoadedRoom;
import org.oldskooler.vibe.game.room.runtime.RoomOccupantSnapshot;
import org.oldskooler.vibe.net.session.Session;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds occupant and status payload bodies for room packets.
 */
public final class RoomOccupantPayloadWriter {

    /**
     * Builds user objects payload.
     * @param occupants the occupants value
     * @return the resulting build user objects payload
     */
    public String buildUserObjectsPayload(List<RoomOccupantSnapshot> occupants) {
        StringBuilder payload = new StringBuilder("\r");
        for (RoomOccupantSnapshot occupant : occupants) {
            Player player = occupant.player();
            if (player == null) {
                continue;
            }

            RoomPosition position = occupant.position();
            payload.append("i:").append(player.getId()).append('\r');
            payload.append("a:").append(player.getId()).append('\r');
            payload.append("n:").append(player.getUsername()).append('\r');
            payload.append("f:").append(player.getFigure()).append('\r');
            payload.append("s:").append(player.getSex()).append('\r');
            payload.append("l:").append(position.x()).append(' ')
                    .append(position.y()).append(' ')
                    .append(formatHeight(position.z())).append('\r');
            if (player.getMotto() != null && !player.getMotto().isBlank()) {
                payload.append("c:").append(player.getMotto()).append('\r');
            }
        }
        return payload.toString();
    }

    /**
     * Builds user status payload.
     * @param occupants the occupants value
     * @return the resulting build user status payload
     */
    public String buildUserStatusPayload(List<RoomOccupantSnapshot> occupants) {
        StringBuilder payload = new StringBuilder();
        for (RoomOccupantSnapshot occupant : occupants) {
            Player player = occupant.player();
            if (player == null) {
                continue;
            }

            RoomPosition position = occupant.position();
            payload.append(player.getId()).append(' ')
                    .append(position.x()).append(',')
                    .append(position.y()).append(',')
                    .append(formatHeight(position.z())).append(',')
                    .append(occupant.bodyRotation()).append(',')
                    .append(occupant.headRotation()).append('/');
            if (occupant.nextPosition() != null) {
                payload.append("mv ")
                        .append(occupant.nextPosition().x()).append(',')
                        .append(occupant.nextPosition().y()).append(',')
                        .append(formatHeight(occupant.nextPosition().z())).append('/');
            }
            payload.append('\r');
        }
        return payload.toString();
    }

    /**
     * Resolves occupants.
     * @param session the session value
     * @param loadedRoom the loaded room value
     * @param visuals the visuals value
     * @return the resulting resolve occupants
     */
    public List<RoomOccupantSnapshot> resolveOccupants(
            Session session,
            LoadedRoom<?> loadedRoom,
            RoomLayoutRegistry.RoomVisuals visuals
    ) {
        if (loadedRoom == null || loadedRoom.isEmpty()) {
            if (session.getPlayer() == null) {
                return List.of();
            }
            return List.of(fallbackSnapshot(session, visuals));
        }

        return resolveOccupants(loadedRoom);
    }

    /**
     * Resolves occupants.
     * @param loadedRoom the loaded room value
     * @return the resulting resolve occupants
     */
    public List<RoomOccupantSnapshot> resolveOccupants(LoadedRoom<?> loadedRoom) {
        List<RoomOccupantSnapshot> occupants = new ArrayList<>(loadedRoom.getOccupantSnapshots());
        occupants.sort(Comparator.comparingInt(RoomOccupantSnapshot::playerId));
        return occupants;
    }

    /**
     * Fallbacks snapshot.
     * @param session the session value
     * @param visuals the visuals value
     * @return the result of this operation
     */
    private RoomOccupantSnapshot fallbackSnapshot(Session session, RoomLayoutRegistry.RoomVisuals visuals) {
        return new RoomOccupantSnapshot(
                session,
                session.getPlayer(),
                new RoomPosition(visuals.doorX(), visuals.doorY(), visuals.doorZ()),
                null,
                visuals.doorDir(),
                visuals.doorDir()
        );
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
