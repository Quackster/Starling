package org.starling.game.room.runtime;

import org.starling.game.player.Player;
import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.net.session.Session;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Mutable live movement state for one player inside a loaded room.
 */
public final class RoomOccupant {

    private final Session session;
    private RoomPosition position;
    private RoomPosition nextPosition;
    private final Deque<RoomPosition> path = new ArrayDeque<>();
    private RoomCoordinate goal;
    private int bodyRotation;
    private int headRotation;

    /**
     * Creates a new RoomOccupant.
     * @param session the session value
     * @param position the position value
     * @param rotation the rotation value
     */
    public RoomOccupant(Session session, RoomPosition position, int rotation) {
        this.session = session;
        this.position = position;
        this.bodyRotation = rotation;
        this.headRotation = rotation;
    }

    /**
     * Returns the session.
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Returns the player.
     * @return the player
     */
    public Player getPlayer() {
        return session == null ? null : session.getPlayer();
    }

    /**
     * Returns the position.
     * @return the position
     */
    public RoomPosition getPosition() {
        return position;
    }

    /**
     * Sets the position.
     * @param position the position value
     */
    public void setPosition(RoomPosition position) {
        this.position = position;
    }

    /**
     * Returns the pathing position.
     * @return the pathing position
     */
    public RoomPosition getPathingPosition() {
        return nextPosition != null ? nextPosition : position;
    }

    /**
     * Returns the next position.
     * @return the next position
     */
    public RoomPosition getNextPosition() {
        return nextPosition;
    }

    /**
     * Sets the next position.
     * @param nextPosition the next position value
     */
    public void setNextPosition(RoomPosition nextPosition) {
        this.nextPosition = nextPosition;
    }

    /**
     * Returns the goal.
     * @return the goal
     */
    public RoomCoordinate getGoal() {
        return goal;
    }

    /**
     * Sets path.
     * @param steps the steps value
     * @param goal the goal value
     */
    public void setPath(List<RoomPosition> steps, RoomCoordinate goal) {
        path.clear();
        path.addAll(steps);
        this.goal = goal;
    }

    /**
     * Peeks next step.
     * @return the result of this operation
     */
    public RoomPosition peekNextStep() {
        return path.peekFirst();
    }

    /**
     * Polls next step.
     * @return the result of this operation
     */
    public RoomPosition pollNextStep() {
        return path.pollFirst();
    }

    /**
     * Returns whether this has queued path.
     * @return whether this has queued path
     */
    public boolean hasQueuedPath() {
        return !path.isEmpty();
    }

    /**
     * Queueds step count.
     * @return the result of this operation
     */
    public int queuedStepCount() {
        return path.size();
    }

    /**
     * Stops walking.
     * @return the result of this operation
     */
    public boolean stopWalking() {
        boolean changed = nextPosition != null || !path.isEmpty() || goal != null;
        nextPosition = null;
        path.clear();
        goal = null;
        return changed;
    }

    /**
     * Finishes pending step.
     * @return the result of this operation
     */
    public boolean finishPendingStep() {
        if (nextPosition == null) {
            return stopWalking();
        }

        boolean changed = !path.isEmpty() || goal != null;
        path.clear();
        goal = null;
        return changed;
    }

    /**
     * Returns the body rotation.
     * @return the body rotation
     */
    public int getBodyRotation() {
        return bodyRotation;
    }

    /**
     * Sets the body rotation.
     * @param bodyRotation the body rotation value
     */
    public void setBodyRotation(int bodyRotation) {
        this.bodyRotation = bodyRotation;
    }

    /**
     * Returns the head rotation.
     * @return the head rotation
     */
    public int getHeadRotation() {
        return headRotation;
    }

    /**
     * Sets the head rotation.
     * @param headRotation the head rotation value
     */
    public void setHeadRotation(int headRotation) {
        this.headRotation = headRotation;
    }

    /**
     * Snapshots.
     * @return the resulting snapshot
     */
    public RoomOccupantSnapshot snapshot() {
        return new RoomOccupantSnapshot(session, getPlayer(), position, nextPosition, bodyRotation, headRotation);
    }
}
