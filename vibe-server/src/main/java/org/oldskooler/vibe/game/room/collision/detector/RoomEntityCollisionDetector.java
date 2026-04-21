package org.oldskooler.vibe.game.room.collision.detector;

import org.oldskooler.vibe.game.room.collision.RoomCollisionDetector;
import org.oldskooler.vibe.game.room.geometry.RoomCoordinate;
import org.oldskooler.vibe.game.room.runtime.RoomOccupantSnapshot;

/**
 * Blocks movement onto tiles currently occupied or reserved by another room occupant.
 */
public final class RoomEntityCollisionDetector implements RoomCollisionDetector {

    /**
     * Evaluates.
     * @param context the context value
     * @param state the state value
     */
    @Override
    public void evaluate(Context context, State state) {
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

    /**
     * Ises mover.
     * @param context the context value
     * @param snapshot the snapshot value
     * @return the result of this operation
     */
    private boolean isMover(Context context, RoomOccupantSnapshot snapshot) {
        if (context.mover().getSession() != null && context.mover().getSession() == snapshot.session()) {
            return true;
        }

        return context.mover().getPlayer() != null
                && snapshot.player() != null
                && context.mover().getPlayer().getId() == snapshot.player().getId();
    }

    /**
     * Occupieses.
     * @param occupied the occupied value
     * @param target the target value
     * @return the result of this operation
     */
    private boolean occupies(RoomCoordinate occupied, RoomCoordinate target) {
        return occupied != null && occupied.equals(target);
    }
}
