package org.starling.game.room.path;

import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;

final class RoomPathNode {

    private final RoomCoordinate coordinate;
    private final RoomPosition position;
    private final int costFromStart;
    private final int estimatedTotalCost;
    private final RoomPathNode previous;

    RoomPathNode(
            RoomCoordinate coordinate,
            RoomPosition position,
            int costFromStart,
            int estimatedTotalCost,
            RoomPathNode previous
    ) {
        this.coordinate = coordinate;
        this.position = position;
        this.costFromStart = costFromStart;
        this.estimatedTotalCost = estimatedTotalCost;
        this.previous = previous;
    }

    RoomCoordinate coordinate() {
        return coordinate;
    }

    RoomPosition position() {
        return position;
    }

    int costFromStart() {
        return costFromStart;
    }

    int estimatedTotalCost() {
        return estimatedTotalCost;
    }

    RoomPathNode previous() {
        return previous;
    }
}
