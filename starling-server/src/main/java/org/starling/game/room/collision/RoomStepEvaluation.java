package org.starling.game.room.collision;

import org.starling.game.room.geometry.RoomPosition;

public record RoomStepEvaluation(boolean allowed, RoomPosition position) {

    public static RoomStepEvaluation blocked() {
        return new RoomStepEvaluation(false, null);
    }

    public static RoomStepEvaluation allowed(RoomPosition position) {
        return new RoomStepEvaluation(true, position);
    }
}
