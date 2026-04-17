package org.starling.game.room.collision;

/**
 * Evaluates one aspect of whether a room movement step is valid.
 */
public interface RoomCollisionDetector {

    void evaluate(RoomCollisionContext context, RoomCollisionState state);
}
