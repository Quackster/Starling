package org.oldskooler.vibe.game.room.path;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.oldskooler.vibe.game.room.collision.RoomCollisionPipeline;
import org.oldskooler.vibe.game.room.collision.RoomCollisionDetector;
import org.oldskooler.vibe.game.room.geometry.RoomCoordinate;
import org.oldskooler.vibe.game.room.geometry.RoomGeometry;
import org.oldskooler.vibe.game.room.geometry.RoomPosition;
import org.oldskooler.vibe.game.room.geometry.RoomTile;
import org.oldskooler.vibe.game.room.runtime.RoomOccupant;
import org.oldskooler.vibe.game.room.runtime.RoomOccupantSnapshot;
import org.oldskooler.vibe.game.room.runtime.WalkableRoom;
import org.oldskooler.vibe.net.session.Session;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the room pathfinder against blocked tiles, height limits, fallback
 * targets, and custom collision detectors.
 */
class RoomPathfinderTest {

    private final RoomPathfinder pathfinder = new RoomPathfinder(RoomCollisionPipeline.defaults());

    /**
     * Findses route around blocked tile.
     */
    @Test
    void findsRouteAroundBlockedTile() {
        TestWalkableRoom room = room(
                "...",
                ".x.",
                "..."
        );
        RoomOccupant mover = occupantAt(0, 0);

        List<RoomPosition> path = pathfinder.findPath(room, mover, new RoomCoordinate(2, 2));

        assertFalse(path.isEmpty());
        assertTrue(path.stream().noneMatch(step -> step.coordinate().equals(new RoomCoordinate(1, 1))));
        assertTrue(path.get(path.size() - 1).coordinate().equals(new RoomCoordinate(2, 2)));
    }

    /**
     * Blockses diagonal corner cutting.
     */
    @Test
    void blocksDiagonalCornerCutting() {
        TestWalkableRoom room = room(
                ".x",
                "x."
        );
        RoomOccupant mover = occupantAt(0, 0);

        List<RoomPosition> path = pathfinder.findPath(room, mover, new RoomCoordinate(1, 1));

        assertTrue(path.isEmpty());
    }

    /**
     * Rejectses steps that are too high.
     */
    @Test
    void rejectsStepsThatAreTooHigh() {
        RoomTile[][] tiles = new RoomTile[][]{
                {RoomTile.open(0), RoomTile.open(3)}
        };
        TestWalkableRoom room = new TestWalkableRoom(new RoomGeometry(
                "test",
                2,
                1,
                tiles,
                Map.of(),
                new RoomPosition(0, 0, 0),
                2
        ));
        RoomOccupant mover = occupantAt(0, 0);

        List<RoomPosition> path = pathfinder.findPath(room, mover, new RoomCoordinate(1, 0));

        assertTrue(path.isEmpty());
    }

    /**
     * Fallses back to nearest reachable tile when goal is blocked.
     */
    @Test
    void fallsBackToNearestReachableTileWhenGoalIsBlocked() {
        TestWalkableRoom room = room(
                "..."
        ).withOccupants(List.of(snapshotAt(2, 0)));
        RoomOccupant mover = occupantAt(0, 0);

        List<RoomPosition> path = pathfinder.findPath(room, mover, new RoomCoordinate(2, 0));

        assertFalse(path.isEmpty());
        assertTrue(path.get(path.size() - 1).coordinate().equals(new RoomCoordinate(1, 0)));
    }

    /**
     * Supportses appending custom collision detectors.
     */
    @Test
    void supportsAppendingCustomCollisionDetectors() {
        RoomCollisionDetector customDetector = (context, state) -> {
            if (context.target().equals(new RoomCoordinate(1, 0))) {
                state.block();
            }
        };
        RoomPathfinder customPathfinder = new RoomPathfinder(RoomCollisionPipeline.defaultsBuilder()
                .addDetector(customDetector)
                .build());
        TestWalkableRoom room = room(
                "..."
        );
        RoomOccupant mover = occupantAt(0, 0);

        List<RoomPosition> path = customPathfinder.findPath(room, mover, new RoomCoordinate(2, 0));

        assertTrue(path.isEmpty());
    }

    /**
     * Rooms.
     * @param rows the rows value
     * @return the resulting room
     */
    private static TestWalkableRoom room(String... rows) {
        RoomTile[][] tiles = new RoomTile[rows.length][rows[0].length()];
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                tiles[y][x] = rows[y].charAt(x) == 'x' ? RoomTile.closed() : RoomTile.open(0);
            }
        }

        return new TestWalkableRoom(new RoomGeometry(
                "test",
                rows[0].length(),
                rows.length,
                tiles,
                Map.of(),
                new RoomPosition(0, 0, 0),
                2
        ));
    }

    /**
     * Occupants at.
     * @param x the x value
     * @param y the y value
     * @return the resulting occupant at
     */
    private static RoomOccupant occupantAt(int x, int y) {
        return new RoomOccupant(new Session(new EmbeddedChannel()), new RoomPosition(x, y, 0), 2);
    }

    /**
     * Snapshots at.
     * @param x the x value
     * @param y the y value
     * @return the resulting snapshot at
     */
    private static RoomOccupantSnapshot snapshotAt(int x, int y) {
        Session session = new Session(new EmbeddedChannel());
        return new RoomOccupantSnapshot(session, null, new RoomPosition(x, y, 0), null, 2, 2);
    }

    private record TestWalkableRoom(RoomGeometry geometry, List<RoomOccupantSnapshot> occupants) implements WalkableRoom {

        /**
         * Creates a new TestWalkableRoom.
         * @param geometry the geometry value
         */
        private TestWalkableRoom(RoomGeometry geometry) {
            this(geometry, List.of());
        }

        /**
         * Returns the geometry.
         * @return the geometry
         */
        @Override
        public RoomGeometry getGeometry() {
            return geometry;
        }

        /**
         * Returns the occupant snapshots.
         * @return the occupant snapshots
         */
        @Override
        public List<RoomOccupantSnapshot> getOccupantSnapshots() {
            return occupants;
        }

        /**
         * Returns a copy with occupants.
         * @param occupants the occupants value
         * @return the result of this operation
         */
        private TestWalkableRoom withOccupants(List<RoomOccupantSnapshot> occupants) {
            return new TestWalkableRoom(geometry, occupants);
        }
    }
}
