package org.starling.game.room.response.occupant;

import org.starling.game.player.Player;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.layout.RoomLayoutRegistry;
import org.starling.game.room.registry.LoadedRoom;
import org.starling.game.room.runtime.RoomOccupantSnapshot;
import org.starling.net.session.Session;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RoomOccupantPayloadWriter {

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

    public List<RoomOccupantSnapshot> resolveOccupants(LoadedRoom<?> loadedRoom) {
        List<RoomOccupantSnapshot> occupants = new ArrayList<>(loadedRoom.getOccupantSnapshots());
        occupants.sort(Comparator.comparingInt(RoomOccupantSnapshot::playerId));
        return occupants;
    }

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

    private String formatHeight(double value) {
        return Math.floor(value) == value ? Integer.toString((int) value) : Double.toString(value);
    }
}
