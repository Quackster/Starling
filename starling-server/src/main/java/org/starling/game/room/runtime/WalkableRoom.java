package org.starling.game.room.runtime;

import org.starling.game.room.geometry.RoomGeometry;

import java.util.List;

public interface WalkableRoom {

    RoomGeometry getGeometry();

    List<RoomOccupantSnapshot> getOccupantSnapshots();
}
