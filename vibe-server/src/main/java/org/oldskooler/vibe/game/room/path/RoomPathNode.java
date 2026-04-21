package org.oldskooler.vibe.game.room.path;

import org.oldskooler.vibe.game.room.geometry.RoomCoordinate;
import org.oldskooler.vibe.game.room.geometry.RoomPosition;

/**
 * Search node used by the room pathfinder while exploring candidate paths.
 */
final class RoomPathNode {

    private final RoomCoordinate coordinate;
    private final RoomPosition position;
    private final int costFromStart;
    private final int estimatedTotalCost;
    private final RoomPathNode previous;

    /**
     * Creates a new RoomPathNode.
     * @param coordinate the coordinate value
     * @param position the position value
     * @param costFromStart the cost from start value
     * @param estimatedTotalCost the estimated total cost value
     * @param previous the previous value
     */
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

    /**
     * Coordinates.
     * @return the result of this operation
     */
    RoomCoordinate coordinate() {
        return coordinate;
    }

    /**
     * Positions.
     * @return the result of this operation
     */
    RoomPosition position() {
        return position;
    }

    /**
     * Costs from start.
     * @return the result of this operation
     */
    int costFromStart() {
        return costFromStart;
    }

    /**
     * Estimateds total cost.
     * @return the result of this operation
     */
    int estimatedTotalCost() {
        return estimatedTotalCost;
    }

    /**
     * Previouses.
     * @return the result of this operation
     */
    RoomPathNode previous() {
        return previous;
    }
}
