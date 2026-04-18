package org.starling.web.view;

import org.starling.storage.entity.UserEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PublicFeatureContentFactory {

    private final UserViewModelFactory userViewModelFactory;

    /**
     * Creates a new PublicFeatureContentFactory.
     * @param userViewModelFactory the user view model factory
     */
    public PublicFeatureContentFactory(UserViewModelFactory userViewModelFactory) {
        this.userViewModelFactory = userViewModelFactory;
    }

    /**
     * Returns the placeholder online friends list.
     * @return the online friends
     */
    public List<String> onlineFriends() {
        return List.of("RetroGuide", "PixelPilot", "Newsie");
    }

    /**
     * Returns the placeholder recommended groups.
     * @return the recommended groups
     */
    public List<Map<String, Object>> recommendedGroups() {
        return List.of(
                Map.of("name", "Starling Builders", "badge", "b0514Xs09114s05013s05014"),
                Map.of("name", "Rare Traders", "badge", "b04124s09113s05013s05014"),
                Map.of("name", "Pixel Collectors", "badge", "b0509Xs09114s05013s05014"),
                Map.of("name", "Rooftop Residents", "badge", "b0404Xs09114s05013s05014")
        );
    }

    /**
     * Returns the public tag list.
     * @return the tag list
     */
    public List<String> tagCloud() {
        return List.of("cms", "retro", "hotel", "community", "lisbon", "news");
    }

    /**
     * Returns the placeholder recommended rooms.
     * @return the recommended room list
     */
    public List<Map<String, Object>> recommendedRooms() {
        return List.of(
                room("Sunset Lounge", "Retro nights and rooftop chats.", "RetroGuide", "room-occupancy-2"),
                room("Neon Loft", "Design swaps and pixel art showcases.", "PixelPilot", "room-occupancy-3"),
                room("Cinema Suite", "Movie trivia, snacks, and comfy corners.", "Newsie", "room-occupancy-4"),
                room("Arcade Den", "High scores, tournaments, and fast reflexes.", "ByteBeat", "room-occupancy-2"),
                room("Pool Deck", "Laid-back hangouts with a summer soundtrack.", "WaveRider", "room-occupancy-1")
        );
    }

    /**
     * Returns community members used by the community landing page.
     * @param currentUser the currently authenticated user, when present
     * @return the community member list
     */
    public List<Map<String, Object>> activeMembers(Optional<UserEntity> currentUser) {
        List<Map<String, Object>> members = new ArrayList<>();
        currentUser.ifPresent(user -> {
            Map<String, Object> currentUserView = new LinkedHashMap<>(userViewModelFactory.create(user));
            currentUserView.put("createdOn", currentUserView.get("memberSince"));
            currentUserView.put("status", "online");
            currentUserView.put("url", "/me");
            members.add(currentUserView);
        });

        members.add(member("RetroGuide", "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64", "Keeping the welcome room lively.", "12 Mar 2025", "online", "/community"));
        members.add(member("PixelPilot", "hr-165-42.hd-190-1.ch-255-66.lg-280-82.sh-305-64", "Always testing a new layout idea.", "18 Feb 2025", "online", "/community"));
        members.add(member("Newsie", "hr-515-45.hd-600-2.ch-255-92.lg-720-82.sh-730-64", "Knows every headline before breakfast.", "07 Jan 2025", "offline", "/community"));
        members.add(member("ByteBeat", "hr-828-61.hd-180-1.ch-210-66.lg-270-82.sh-290-91", "Arcade tournaments every Friday.", "03 Apr 2025", "online", "/community"));
        members.add(member("WaveRider", "hr-100-42.hd-180-1.ch-210-66.lg-270-82.sh-290-91", "Pool deck DJ and sunset host.", "27 Mar 2025", "offline", "/community"));
        members.add(member("Roomsmith", "hr-100-61.hd-600-1.ch-255-62.lg-280-82.sh-300-64", "Building cozy spaces for every event.", "21 Apr 2025", "online", "/community"));
        return members;
    }

    private static Map<String, Object> room(String name, String description, String owner, String occupancyClass) {
        return Map.of(
                "name", name,
                "description", description,
                "owner", owner,
                "occupancyClass", occupancyClass
        );
    }

    private static Map<String, Object> member(
            String name,
            String figure,
            String motto,
            String createdOn,
            String status,
            String url
    ) {
        return Map.of(
                "name", name,
                "figure", figure,
                "motto", motto,
                "createdOn", createdOn,
                "status", status,
                "url", url
        );
    }
}
