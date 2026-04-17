package org.starling.game.room.path;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.starling.game.room.collision.RoomCollisionPipeline;
import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomGeometry;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.geometry.RoomTile;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.RoomOccupantSnapshot;
import org.starling.game.room.runtime.WalkableRoom;
import org.starling.net.session.Session;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomPathfinderTest {

    private final RoomPathfinder pathfinder = new RoomPathfinder(RoomCollisionPipeline.defaults());

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

    private static RoomOccupant occupantAt(int x, int y) {
        return new RoomOccupant(new Session(new EmbeddedChannel()), new RoomPosition(x, y, 0), 2);
    }

    private record TestWalkableRoom(RoomGeometry geometry) implements WalkableRoom {

        @Override
        public RoomGeometry getGeometry() {
            return geometry;
        }

        @Override
        public List<RoomOccupantSnapshot> getOccupantSnapshots() {
            return List.of();
        }
    }
}
