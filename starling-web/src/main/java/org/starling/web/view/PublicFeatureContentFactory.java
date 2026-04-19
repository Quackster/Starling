package org.starling.web.view;

import org.starling.storage.entity.UserEntity;
import org.starling.web.site.SiteBranding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PublicFeatureContentFactory {

    private final UserViewModelFactory userViewModelFactory;
    private final SiteBranding siteBranding;

    /**
     * Creates a new PublicFeatureContentFactory.
     * @param userViewModelFactory the user view model factory
     */
    public PublicFeatureContentFactory(UserViewModelFactory userViewModelFactory, SiteBranding siteBranding) {
        this.userViewModelFactory = userViewModelFactory;
        this.siteBranding = siteBranding;
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
                Map.of("name", siteBranding.siteName() + " Builders", "badge", "b0514Xs09114s05013s05014"),
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
                room(101, "Sunset Lounge", "Retro nights and rooftop chats.", "RetroGuide", "room-occupancy-2"),
                room(102, "Neon Loft", "Design swaps and pixel art showcases.", "PixelPilot", "room-occupancy-3"),
                room(103, "Cinema Suite", "Movie trivia, snacks, and comfy corners.", "Newsie", "room-occupancy-4"),
                room(104, "Arcade Den", "High scores, tournaments, and fast reflexes.", "ByteBeat", "room-occupancy-2"),
                room(105, "Pool Deck", "Laid-back hangouts with a summer soundtrack.", "WaveRider", "room-occupancy-1")
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
        members.add(member("CosmicRay", "hr-802-45.hd-180-1.ch-215-92.lg-275-82.sh-295-64", "Always planning the next rooftop party.", "15 Dec 2024", "online", "/community"));
        members.add(member("VelvetVox", "hr-893-61.hd-600-2.ch-255-92.lg-705-82.sh-730-64", "Curating the late-night lounge playlist.", "29 Nov 2024", "offline", "/community"));
        members.add(member("PixelBloom", "hr-165-42.hd-600-1.ch-3030-92.lg-275-92.sh-290-64", "Collecting badges and color palettes.", "11 Oct 2024", "online", "/community"));
        members.add(member("NightOwl", "hr-100-42.hd-190-1.ch-210-66.lg-285-82.sh-295-91", "Usually found in the cinema queue.", "02 Sep 2024", "offline", "/community"));
        members.add(member("TradeWind", "hr-515-45.hd-180-1.ch-255-66.lg-720-82.sh-730-64", "Rare trader with a soft spot for neon.", "18 Aug 2024", "online", "/community"));
        members.add(member("CloudDeck", "hr-828-61.hd-600-2.ch-210-92.lg-270-82.sh-290-64", "Keeps the pool deck booked and busy.", "09 Jul 2024", "offline", "/community"));
        members.add(member("LunaByte", "hr-100-61.hd-180-2.ch-255-92.lg-275-82.sh-290-91", "Arcade speedrunner and trivia host.", "22 Jun 2024", "online", "/community"));
        members.add(member("SketchPad", "hr-165-42.hd-190-1.ch-255-92.lg-280-82.sh-300-64", "Posting room layouts every afternoon.", "30 May 2024", "online", "/community"));
        members.add(member("HarborLight", "hr-100-61.hd-600-1.ch-210-66.lg-270-82.sh-290-64", "Likes quiet corners and warm lighting.", "13 Apr 2024", "offline", "/community"));
        members.add(member("PulseWave", "hr-893-61.hd-180-1.ch-215-66.lg-710-82.sh-730-64", "Resident DJ for the after-hours crowd.", "26 Mar 2024", "online", "/community"));
        members.add(member("RoomRunner", "hr-515-45.hd-190-2.ch-210-92.lg-720-82.sh-730-91", "Always hopping between open rooms.", "17 Feb 2024", "online", "/community"));
        members.add(member("BlueSkies", "hr-100-42.hd-600-2.ch-255-66.lg-280-82.sh-300-64", "Collects postcards from every event.", "05 Jan 2024", "offline", "/community"));
        return members;
    }

    private static Map<String, Object> room(int roomId, String name, String description, String owner, String occupancyClass) {
        return Map.of(
                "roomId", roomId,
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
