package org.oldskooler.vibe.game.room.collision;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.oldskooler.vibe.game.room.geometry.RoomCoordinate;
import org.oldskooler.vibe.game.room.geometry.RoomGeometry;
import org.oldskooler.vibe.game.room.geometry.RoomPosition;
import org.oldskooler.vibe.game.room.geometry.RoomTile;
import org.oldskooler.vibe.game.room.path.RoomPathfinder;
import org.oldskooler.vibe.game.room.runtime.RoomOccupant;
import org.oldskooler.vibe.game.room.runtime.RoomOccupantSnapshot;
import org.oldskooler.vibe.game.room.runtime.WalkableRoom;
import org.oldskooler.vibe.net.session.Session;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the collision registry snapshot reflects appended detectors
 * and feeds those detectors into pathfinding.
 */
class RoomCollisionRegistryTest {

    /**
     * Appendeds detector changes snapshot pipeline.
     */
    @Test
    void appendedDetectorChangesSnapshotPipeline() {
        RoomCollisionRegistry registry = new RoomCollisionRegistry();
        registry.appendDetector((context, state) -> {
            if (context.target().equals(new RoomCoordinate(1, 0))) {
                state.block();
            }
        });

        RoomPathfinder pathfinder = new RoomPathfinder(registry.snapshotPipeline());
        List<RoomPosition> path = pathfinder.findPath(room("..."), occupantAt(0, 0), new RoomCoordinate(2, 0));

        assertEquals(List.of(), path);
    }

    /**
     * Rooms.
     * @param rows the rows value
     * @return the resulting room
     */
    private static WalkableRoom room(String... rows) {
        RoomTile[][] tiles = new RoomTile[rows.length][rows[0].length()];
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                tiles[y][x] = rows[y].charAt(x) == 'x' ? RoomTile.closed() : RoomTile.open(0);
            }
        }

        RoomGeometry geometry = new RoomGeometry(
                "test",
                rows[0].length(),
                rows.length,
                tiles,
                Map.of(),
                new RoomPosition(0, 0, 0),
                2
        );
        return new WalkableRoom() {
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
                return List.of();
            }
        };
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
}
