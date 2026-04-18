package org.starling.game.room.runtime;

import org.starling.game.room.collision.RoomCollisionPipeline;
import org.starling.game.room.collision.RoomCollisionRegistry;
import org.starling.game.room.collision.RoomCollisionDetector;
import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.path.RoomDirection;
import org.starling.game.room.path.RoomPathfinder;
import org.starling.game.room.registry.LoadedRoom;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.net.session.Session;

import java.util.List;

/**
 * Coordinates room walking requests and the advancement of live room occupants.
 */
public final class RoomMovementService {

    private static final RoomMovementService INSTANCE = new RoomMovementService(
            RoomRegistry.getInstance(),
            RoomCollisionRegistry.getInstance()
    );

    private final RoomRegistry roomRegistry;
    private final RoomCollisionRegistry collisionRegistry;

    /**
     * Creates a new RoomMovementService.
     * @param roomRegistry the room registry value
     * @param responses the responses value
     * @param collisionRegistry the collision registry value
     */
    private RoomMovementService(
            RoomRegistry roomRegistry,
            RoomCollisionRegistry collisionRegistry
    ) {
        this.roomRegistry = roomRegistry;
        this.collisionRegistry = collisionRegistry;
    }

    /**
     * Returns the instance.
     * @return the instance
     */
    public static RoomMovementService getInstance() {
        return INSTANCE;
    }

    /**
     * Walks.
     * @param session the session value
     * @param x the x value
     * @param y the y value
     * @return the result of this operation
     */
    public boolean walk(Session session, int x, int y) {
        LoadedRoom<?> room = resolveActiveRoom(session);
        if (room == null) {
            return false;
        }

        RoomCollisionPipeline collisionPipeline = collisionRegistry.snapshotPipeline();
        RoomPathfinder pathfinder = new RoomPathfinder(collisionPipeline);
        boolean broadcastStatus = false;
        synchronized (room) {
            RoomOccupant occupant = room.getOccupant(session);
            RoomPosition pathingPosition = occupant == null ? null : occupant.getPathingPosition();
            if (occupant == null || occupant.getPosition() == null || pathingPosition == null) {
                return false;
            }

            RoomCoordinate goal = new RoomCoordinate(x, y);
            if (goal.equals(pathingPosition.coordinate())) {
                if (occupant.getNextPosition() == null) {
                    broadcastStatus = occupant.stopWalking();
                } else {
                    occupant.finishPendingStep();
                }
            } else {
                List<RoomPosition> path = pathfinder.findPath(room, occupant, goal);
                if (path.isEmpty()) {
                    return false;
                }
                occupant.setPath(path, goal);
            }
        }

        return broadcastStatus || true;
    }

    /**
     * Stops walking.
     * @param session the session value
     * @return the result of this operation
     */
    public boolean stopWalking(Session session) {
        LoadedRoom<?> room = resolveActiveRoom(session);
        if (room == null) {
            return false;
        }

        boolean changed;
        synchronized (room) {
            RoomOccupant occupant = room.getOccupant(session);
            changed = occupant != null && occupant.stopWalking();
        }

        return changed;
    }

    /**
     * Ticks loaded rooms.
     */
    public void tickLoadedRooms() {
        for (LoadedRoom<?> room : roomRegistry.loadedRooms()) {
            tickRoom(room);
        }
    }

    /**
     * Ticks room.
     * @param room the room value
     */
    public void tickRoom(LoadedRoom<?> room) {
        RoomCollisionPipeline collisionPipeline = collisionRegistry.snapshotPipeline();
        boolean changed = false;
        synchronized (room) {
            for (RoomOccupant occupant : room.getOccupantUnits()) {
                changed |= advanceOccupant(room, occupant, collisionPipeline);
            }
        }

    }

    /**
     * Advances occupant.
     * @param room the room value
     * @param occupant the occupant value
     * @param collisionPipeline the collision pipeline value
     * @return the result of this operation
     */
    private boolean advanceOccupant(LoadedRoom<?> room, RoomOccupant occupant, RoomCollisionPipeline collisionPipeline) {
        boolean changed = false;
        RoomPosition current = occupant.getPosition();

        RoomPosition pending = occupant.getNextPosition();
        if (pending != null) {
            RoomCollisionDetector.Evaluation evaluation = collisionPipeline.evaluateStep(
                    room,
                    occupant,
                    current,
                    pending.coordinate(),
                    occupant.getGoal() == null ? pending.coordinate() : occupant.getGoal(),
                    !occupant.hasQueuedPath()
            );
            if (!evaluation.allowed()) {
                return occupant.stopWalking();
            }

            occupant.setPosition(evaluation.position());
            occupant.setNextPosition(null);
            current = evaluation.position();
            changed = true;
        }

        RoomPosition queuedStep = occupant.peekNextStep();
        if (queuedStep == null) {
            if (occupant.getGoal() != null && occupant.getGoal().equals(current.coordinate())) {
                occupant.stopWalking();
            }
            return changed;
        }

        boolean finalStep = occupant.queuedStepCount() == 1;
        RoomCollisionDetector.Evaluation evaluation = collisionPipeline.evaluateStep(
                room,
                occupant,
                current,
                queuedStep.coordinate(),
                occupant.getGoal() == null ? queuedStep.coordinate() : occupant.getGoal(),
                finalStep
        );
        if (!evaluation.allowed()) {
            return occupant.stopWalking() || changed;
        }

        occupant.pollNextStep();
        occupant.setNextPosition(evaluation.position());
        int rotation = RoomDirection.fromStep(current.coordinate(), evaluation.position().coordinate());
        occupant.setBodyRotation(rotation);
        occupant.setHeadRotation(rotation);
        return true;
    }

    /**
     * Resolves active room.
     * @param session the session value
     * @return the resulting resolve active room
     */
    private LoadedRoom<?> resolveActiveRoom(Session session) {
        Session.RoomPresence presence = session == null ? null : session.getRoomPresence();
        if (presence == null || !presence.active()) {
            return null;
        }
        return roomRegistry.find(presence.type(), presence.roomId());
    }
}
