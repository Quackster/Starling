package org.starling.web.feature.me.content;

import java.util.List;
import java.util.Map;

public final class MePageContentFactory {

    /**
     * Returns the placeholder online friends list.
     * @return the online friends
     */
    public List<String> onlineFriends() {
        return List.of("RetroGuide", "PixelPilot", "Newsie");
    }

    /**
     * Returns the welcome room list shown after registration.
     * @return the welcome rooms
     */
    public List<Map<String, Object>> welcomeRooms() {
        return List.of(
                Map.of("id", 0, "label", "Sunset Lounge"),
                Map.of("id", 1, "label", "Neon Loft"),
                Map.of("id", 2, "label", "Rooftop Club"),
                Map.of("id", 3, "label", "Cinema Suite"),
                Map.of("id", 4, "label", "Arcade Den"),
                Map.of("id", 5, "label", "Pool Deck")
        );
    }
}
