package org.starling.web.cms.bootstrap.seed.data;

import java.util.List;

public final class CmsCommunitySeedCatalog {

    private static final List<RoomSeed> ROOMS = List.of(
            new RoomSeed("Welcome Lounge", "Start here, meet people, and get your bearings.", 18),
            new RoomSeed("Skyline Suite", "Rooftop conversations and city-night screenshots.", 14),
            new RoomSeed("Battle Arcade", "Retro cabinets, tournaments, and bragging rights.", 11),
            new RoomSeed("Library Lounge", "A quieter social room with warm corners and reading nooks.", 8),
            new RoomSeed("Pool Deck", "Sunset chats and laid-back summer vibes.", 6),
            new RoomSeed("Pixel Workshop", "Builders swapping layouts, palettes, and room ideas.", 10)
    );
    private static final List<GroupSeed> GROUPS = List.of(
            new GroupSeed("Welcome Crew", "b0514Xs09114s05013s05014", "Helping new players settle into the hotel."),
            new GroupSeed("Skyline Social", "b04124s09113s05013s05014", "Late-night chats, music picks, and rooftop hangs."),
            new GroupSeed("Pixel Builders", "b0509Xs09114s05013s05014", "Builders sharing layouts, feedback, and room tours."),
            new GroupSeed("Arcade League", "b0601Xs09114s05013s05014", "Competitive events and high-score nights."),
            new GroupSeed("Poolside Club", "b0603Xs09114s05013s05014", "Relaxed socials, summer rooms, and snapshots."),
            new GroupSeed("Newswire", "b0604Xs09114s05013s05014", "Community highlights, updates, and event recaps.")
    );

    /**
     * Creates a new CmsCommunitySeedCatalog.
     */
    private CmsCommunitySeedCatalog() {}

    /**
     * Returns bootstrap room seeds.
     * @return the room seeds
     */
    public static List<RoomSeed> rooms() {
        return ROOMS;
    }

    /**
     * Returns bootstrap group seeds.
     * @return the group seeds
     */
    public static List<GroupSeed> groups() {
        return GROUPS;
    }

    public record RoomSeed(String name, String description, int currentUsers) {}

    public record GroupSeed(String name, String badge, String description) {}
}
