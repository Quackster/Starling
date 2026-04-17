package org.starling.game.room.collision;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomGeometry;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.geometry.RoomTile;
import org.starling.game.room.path.RoomPathfinder;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.RoomOccupantSnapshot;
import org.starling.game.room.runtime.WalkableRoom;
import org.starling.net.session.Session;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomCollisionRegistryTest {

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
            @Override
            public RoomGeometry getGeometry() {
                return geometry;
            }

            @Override
            public List<RoomOccupantSnapshot> getOccupantSnapshots() {
                return List.of();
            }
        };
    }

    private static RoomOccupant occupantAt(int x, int y) {
        return new RoomOccupant(new Session(new EmbeddedChannel()), new RoomPosition(x, y, 0), 2);
    }
}
