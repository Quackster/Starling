package org.starling.game.room.response.publicspace;

import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ServerMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes legacy raw public-room furniture layouts into room object packets.
 */
final class PublicRoomFurnitureSerializer {

    ServerMessage buildObjectsMessage(String rawFurniture) {
        List<FurnitureItem> furnitureItems = parse(rawFurniture);
        ServerMessage message = new ServerMessage(OutgoingPackets.ROOM_OBJECTS).writeInt(furnitureItems.size());
        for (FurnitureItem furnitureItem : furnitureItems) {
            furnitureItem.writeTo(message);
        }
        return message;
    }

    private List<FurnitureItem> parse(String rawFurniture) {
        if (rawFurniture == null || rawFurniture.isBlank()) {
            return List.of();
        }

        String normalized = rawFurniture
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        String[] lines = normalized.split("\n");

        List<FurnitureItem> furnitureItems = new ArrayList<>();
        for (String line : lines) {
            FurnitureItem parsed = parseLine(line);
            if (parsed != null) {
                furnitureItems.add(parsed);
            }
        }
        return furnitureItems;
    }

    private FurnitureItem parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length < 6) {
            return null;
        }

        String extras = "";
        if (parts.length > 6) {
            extras = String.join(" ", java.util.Arrays.copyOfRange(parts, 6, parts.length));
        }

        return new FurnitureItem(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], extras);
    }

    /**
     * Parsed legacy furniture row ready to be written back to the client wire format.
     */
    private record FurnitureItem(
            String instanceId,
            String sprite,
            String x,
            String y,
            String z,
            String direction,
            String extras
    ) {
        private void writeTo(ServerMessage message) {
            message.writeRaw(instanceId + " ");
            message.writeString(sprite);
            message.writeRaw(x + " " + y + " " + z + " " + direction);
            if (extras != null && !extras.isBlank()) {
                message.writeRaw(" " + extras);
            }
            message.writeRaw("\r");
        }
    }
}
