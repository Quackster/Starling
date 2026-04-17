package org.starling.game.room.runtime;

import org.starling.game.room.geometry.RoomGeometry;

import java.util.List;

/**
 * Read-only view of the room data needed by pathfinding and collision checks.
 */
public interface WalkableRoom {

    RoomGeometry getGeometry();

    List<RoomOccupantSnapshot> getOccupantSnapshots();
}
