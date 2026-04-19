package org.starling.web.feature.community.view;

import org.starling.storage.entity.UserEntity;
import org.starling.web.user.view.UserViewModelFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CommunityWidgetsFactory {

    private final UserViewModelFactory userViewModelFactory;

    /**
     * Creates a new CommunityWidgetsFactory.
     * @param userViewModelFactory the user view model factory
     */
    public CommunityWidgetsFactory(UserViewModelFactory userViewModelFactory) {
        this.userViewModelFactory = userViewModelFactory;
    }

    /**
     * Returns the top rated rooms.
     * @return the resulting room list
     */
    public List<Map<String, Object>> topRatedRooms() {
        return List.of(
                room(201, "Skyline Suite", "Open views and rooftop conversations.", "RetroGuide", "room-occupancy-5"),
                room(202, "Grand Foyer", "A busy social hub for hotel regulars.", "Newsie", "room-occupancy-4"),
                room(203, "Battle Arcade", "Competitive cabinets and cheering crowds.", "ByteBeat", "room-occupancy-4"),
                room(204, "Emerald Gardens", "Quiet corners and evening walks.", "WaveRider", "room-occupancy-3"),
                room(205, "Pixel Workshop", "Design jams and furniture mockups.", "PixelPilot", "room-occupancy-3"),
                room(206, "The Exchange", "Collector chat and trade showcases.", "TradeWind", "room-occupancy-4"),
                room(207, "Neon Rooftop", "Night lighting and chilled playlists.", "PulseWave", "room-occupancy-2"),
                room(208, "Cinema Club", "Trailers, watch parties, and polls.", "NightOwl", "room-occupancy-3"),
                room(209, "Poolside Terrace", "Sunset hangouts by the water.", "CloudDeck", "room-occupancy-2"),
                room(210, "Creators Alley", "Build competitions every weekend.", "Roomsmith", "room-occupancy-5"),
                room(211, "Trading Post", "Rare deals and item spotlights.", "TradeWind", "room-occupancy-4"),
                room(212, "Blue Lounge", "Low-key talks and easy company.", "BlueSkies", "room-occupancy-2")
        );
    }

    /**
     * Returns the recommended rooms.
     * @return the resulting room list
     */
    public List<Map<String, Object>> recommendedRooms() {
        return List.of(
                room(101, "Sunset Lounge", "Retro nights and rooftop chats.", "RetroGuide", "room-occupancy-2"),
                room(102, "Neon Loft", "Design swaps and pixel art showcases.", "PixelPilot", "room-occupancy-3"),
                room(103, "Cinema Suite", "Movie trivia, snacks, and comfy corners.", "Newsie", "room-occupancy-4"),
                room(104, "Arcade Den", "High scores, tournaments, and fast reflexes.", "ByteBeat", "room-occupancy-2"),
                room(105, "Pool Deck", "Laid-back hangouts with a summer soundtrack.", "WaveRider", "room-occupancy-1"),
                room(106, "Pixel Patio", "Palette swaps and creative showcases.", "PixelBloom", "room-occupancy-2"),
                room(107, "Trivia Theater", "Daily quizzes and badge challenges.", "LunaByte", "room-occupancy-3"),
                room(108, "Harbor Cafe", "Coffee chats and community catchups.", "HarborLight", "room-occupancy-1"),
                room(109, "Trade Lounge", "Friendly swaps and rare finds.", "TradeWind", "room-occupancy-3"),
                room(110, "Late Night Radio", "DJ sets and dedication shout-outs.", "PulseWave", "room-occupancy-2"),
                room(111, "Sketch Studio", "Layouts, moodboards, and ideas.", "SketchPad", "room-occupancy-2"),
                room(112, "Cloud Deck", "Skyline views and calm conversations.", "CloudDeck", "room-occupancy-1")
        );
    }

    /**
     * Returns the hot groups.
     * @return the resulting group list
     */
    public List<Map<String, Object>> hotGroups() {
        return List.of(
                group(1, "Habbo Builders", "b0514Xs09114s05013s05014"),
                group(2, "Rare Traders", "b04124s09113s05013s05014"),
                group(3, "Pixel Collectors", "b0509Xs09114s05013s05014"),
                group(4, "Rooftop Residents", "b0404Xs09114s05013s05014"),
                group(5, "Arcade League", "b0601Xs09114s05013s05014"),
                group(6, "Cinema Club", "b0602Xs09114s05013s05014"),
                group(7, "Poolside Society", "b0603Xs09114s05013s05014"),
                group(8, "Newsroom Team", "b0604Xs09114s05013s05014"),
                group(9, "Music Makers", "b0605Xs09114s05013s05014"),
                group(10, "Event Crew", "b0606Xs09114s05013s05014"),
                group(11, "Trade Hall", "b0607Xs09114s05013s05014"),
                group(12, "Night Owls", "b0608Xs09114s05013s05014")
        );
    }

    /**
     * Returns recent discussion topics.
     * @return the resulting topic list
     */
    public List<Map<String, Object>> recentTopics() {
        return List.of(
                topic("Tonight's rooftop meetup", List.of(1, 2, 3)),
                topic("Best builder layouts this week", List.of(1, 2, 3, 4)),
                topic("Show us your rarest trade", List.of(1, 2)),
                topic("Which room deserves a spotlight?", List.of(1, 2, 3)),
                topic("Community event ideas for Sunday", List.of(1, 2, 3, 4, 5)),
                topic("Favorite public room right now", List.of(1, 2)),
                topic("Lisbon theme appreciation thread", List.of(1, 2, 3)),
                topic("What should the next badge theme be?", List.of(1, 2, 3, 4)),
                topic("Best effect combinations", List.of(1, 2)),
                topic("Share your best welcome message", List.of(1, 2, 3)),
                topic("Late night radio requests", List.of(1, 2, 3)),
                topic("Screenshot contest submissions", List.of(1, 2)),
                topic("Trade etiquette for new players", List.of(1, 2, 3)),
                topic("Recommended rooms this weekend", List.of(1, 2)),
                topic("Fansite spotlight nominations", List.of(1, 2, 3))
        );
    }

    /**
     * Returns the active community members.
     * @param currentUser the authenticated user, when present
     * @return the resulting member list
     */
    public List<Map<String, Object>> activeMembers(Optional<UserEntity> currentUser) {
        List<Map<String, Object>> members = new ArrayList<>();
        currentUser.ifPresent(user -> {
            Map<String, Object> currentUserView = new LinkedHashMap<>(userViewModelFactory.create(user));
            currentUserView.put("createdOn", currentUserView.get("memberSince"));
            currentUserView.put("status", "online");
            currentUserView.put("url", "/home/" + user.getUsername());
            members.add(currentUserView);
        });

        members.add(member("RetroGuide", "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64", "Keeping the welcome room lively.", "12 Mar 2025", "online"));
        members.add(member("PixelPilot", "hr-165-42.hd-190-1.ch-255-66.lg-280-82.sh-305-64", "Always testing a new layout idea.", "18 Feb 2025", "online"));
        members.add(member("Newsie", "hr-515-45.hd-600-2.ch-255-92.lg-720-82.sh-730-64", "Knows every headline before breakfast.", "07 Jan 2025", "offline"));
        members.add(member("ByteBeat", "hr-828-61.hd-180-1.ch-210-66.lg-270-82.sh-290-91", "Arcade tournaments every Friday.", "03 Apr 2025", "online"));
        members.add(member("WaveRider", "hr-100-42.hd-180-1.ch-210-66.lg-270-82.sh-290-91", "Pool deck DJ and sunset host.", "27 Mar 2025", "offline"));
        members.add(member("Roomsmith", "hr-100-61.hd-600-1.ch-255-62.lg-280-82.sh-300-64", "Building cozy spaces for every event.", "21 Apr 2025", "online"));
        members.add(member("CosmicRay", "hr-802-45.hd-180-1.ch-215-92.lg-275-82.sh-295-64", "Always planning the next rooftop party.", "15 Dec 2024", "online"));
        members.add(member("VelvetVox", "hr-893-61.hd-600-2.ch-255-92.lg-705-82.sh-730-64", "Curating the late-night lounge playlist.", "29 Nov 2024", "offline"));
        members.add(member("PixelBloom", "hr-165-42.hd-600-1.ch-3030-92.lg-275-92.sh-290-64", "Collecting badges and color palettes.", "11 Oct 2024", "online"));
        members.add(member("NightOwl", "hr-100-42.hd-190-1.ch-210-66.lg-285-82.sh-295-91", "Usually found in the cinema queue.", "02 Sep 2024", "offline"));
        members.add(member("TradeWind", "hr-515-45.hd-180-1.ch-255-66.lg-720-82.sh-730-64", "Rare trader with a soft spot for neon.", "18 Aug 2024", "online"));
        members.add(member("CloudDeck", "hr-828-61.hd-600-2.ch-210-92.lg-270-82.sh-290-64", "Keeps the pool deck booked and busy.", "09 Jul 2024", "offline"));
        members.add(member("LunaByte", "hr-100-61.hd-180-2.ch-255-92.lg-275-82.sh-290-91", "Arcade speedrunner and trivia host.", "22 Jun 2024", "online"));
        members.add(member("SketchPad", "hr-165-42.hd-190-1.ch-255-92.lg-280-82.sh-300-64", "Posting room layouts every afternoon.", "30 May 2024", "online"));
        members.add(member("HarborLight", "hr-100-61.hd-600-1.ch-210-66.lg-270-82.sh-290-64", "Likes quiet corners and warm lighting.", "13 Apr 2024", "offline"));
        members.add(member("PulseWave", "hr-893-61.hd-180-1.ch-215-66.lg-710-82.sh-730-64", "Resident DJ for the after-hours crowd.", "26 Mar 2024", "online"));
        members.add(member("RoomRunner", "hr-515-45.hd-190-2.ch-210-92.lg-720-82.sh-730-91", "Always hopping between open rooms.", "17 Feb 2024", "online"));
        members.add(member("BlueSkies", "hr-100-42.hd-600-2.ch-255-66.lg-280-82.sh-300-64", "Collects postcards from every event.", "05 Jan 2024", "offline"));
        return members.stream().limit(18).toList();
    }

    private static Map<String, Object> room(int roomId, String name, String description, String owner, String occupancyClass) {
        return Map.of(
                "roomId", roomId,
                "name", name,
                "description", description,
                "owner", owner,
                "ownerUrl", "/home/" + owner,
                "occupancyClass", occupancyClass
        );
    }

    private static Map<String, Object> group(int id, String name, String badge) {
        return Map.of(
                "id", id,
                "name", name,
                "badge", badge,
                "url", "/community"
        );
    }

    private static Map<String, Object> topic(String title, List<Integer> pages) {
        return Map.of(
                "title", title,
                "url", "/community",
                "pages", pages
        );
    }

    private static Map<String, Object> member(String name, String figure, String motto, String createdOn, String status) {
        return Map.of(
                "name", name,
                "figure", figure,
                "motto", motto,
                "createdOn", createdOn,
                "status", status,
                "url", "/home/" + name
        );
    }
}
