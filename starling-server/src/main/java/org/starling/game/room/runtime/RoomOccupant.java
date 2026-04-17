package org.starling.game.room.runtime;

import org.starling.game.player.Player;
import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.net.session.Session;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class RoomOccupant {

    private final Session session;
    private RoomPosition position;
    private RoomPosition nextPosition;
    private final Deque<RoomPosition> path = new ArrayDeque<>();
    private RoomCoordinate goal;
    private int bodyRotation;
    private int headRotation;

    public RoomOccupant(Session session, RoomPosition position, int rotation) {
        this.session = session;
        this.position = position;
        this.bodyRotation = rotation;
        this.headRotation = rotation;
    }

    public Session getSession() {
        return session;
    }

    public Player getPlayer() {
        return session == null ? null : session.getPlayer();
    }

    public RoomPosition getPosition() {
        return position;
    }

    public void setPosition(RoomPosition position) {
        this.position = position;
    }

    public RoomPosition getNextPosition() {
        return nextPosition;
    }

    public void setNextPosition(RoomPosition nextPosition) {
        this.nextPosition = nextPosition;
    }

    public RoomCoordinate getGoal() {
        return goal;
    }

    public void setPath(List<RoomPosition> steps, RoomCoordinate goal) {
        path.clear();
        path.addAll(steps);
        this.goal = goal;
        this.nextPosition = null;
    }

    public RoomPosition peekNextStep() {
        return path.peekFirst();
    }

    public RoomPosition pollNextStep() {
        return path.pollFirst();
    }

    public boolean hasQueuedPath() {
        return !path.isEmpty();
    }

    public int queuedStepCount() {
        return path.size();
    }

    public boolean stopWalking() {
        boolean changed = nextPosition != null || !path.isEmpty() || goal != null;
        nextPosition = null;
        path.clear();
        goal = null;
        return changed;
    }

    public int getBodyRotation() {
        return bodyRotation;
    }

    public void setBodyRotation(int bodyRotation) {
        this.bodyRotation = bodyRotation;
    }

    public int getHeadRotation() {
        return headRotation;
    }

    public void setHeadRotation(int headRotation) {
        this.headRotation = headRotation;
    }

    public RoomOccupantSnapshot snapshot() {
        return new RoomOccupantSnapshot(session, getPlayer(), position, nextPosition, bodyRotation, headRotation);
    }
}
