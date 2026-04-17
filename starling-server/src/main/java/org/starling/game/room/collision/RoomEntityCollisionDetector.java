package org.starling.game.room.collision;

import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.runtime.RoomOccupantSnapshot;

public final class RoomEntityCollisionDetector implements RoomCollisionDetector {

    @Override
    public void evaluate(RoomCollisionContext context, RoomCollisionState state) {
        for (RoomOccupantSnapshot snapshot : context.room().getOccupantSnapshots()) {
            if (isMover(context, snapshot)) {
                continue;
            }

            if (occupies(snapshot.position() == null ? null : snapshot.position().coordinate(), context.target())
                    || occupies(snapshot.nextPosition() == null ? null : snapshot.nextPosition().coordinate(), context.target())) {
                state.block();
                return;
            }
        }
    }

    private boolean isMover(RoomCollisionContext context, RoomOccupantSnapshot snapshot) {
        if (context.mover().getSession() != null && context.mover().getSession() == snapshot.session()) {
            return true;
        }

        return context.mover().getPlayer() != null
                && snapshot.player() != null
                && context.mover().getPlayer().getId() == snapshot.player().getId();
    }

    private boolean occupies(RoomCoordinate occupied, RoomCoordinate target) {
        return occupied != null && occupied.equals(target);
    }
}
