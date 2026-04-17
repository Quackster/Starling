package org.starling.game.room.collision;

public interface RoomCollisionDetector {

    void evaluate(RoomCollisionContext context, RoomCollisionState state);
}
