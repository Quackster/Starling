package org.oldskooler.vibe.game.room.runtime;

import org.oldskooler.vibe.game.room.geometry.RoomGeometry;

import java.util.List;

/**
 * Read-only view of the room data needed by pathfinding and collision checks.
 */
public interface WalkableRoom {

    /**
     * Returns the geometry.
     * @return the geometry
     */
    RoomGeometry getGeometry();

    /**
     * Returns the occupant snapshots.
     * @return the occupant snapshots
     */
    List<RoomOccupantSnapshot> getOccupantSnapshots();
}
