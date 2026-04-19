package org.starling.web.feature.community.view;

import org.starling.storage.dao.GroupDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.GroupEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.UserEntity;
import org.starling.web.user.view.UserViewModelFactory;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CommunityWidgetsFactory {

    private static final DateTimeFormatter ACTIVE_MEMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

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
        return RoomDao.findTopRated(12).stream()
                .map(this::room)
                .toList();
    }

    /**
     * Returns the recommended rooms.
     * @return the resulting room list
     */
    public List<Map<String, Object>> recommendedRooms() {
        return RoomDao.findRecommended(12).stream()
                .map(this::room)
                .toList();
    }

    /**
     * Returns the hot groups.
     * @return the resulting group list
     */
    public List<Map<String, Object>> hotGroups() {
        return GroupDao.listHot(12).stream()
                .map(this::group)
                .toList();
    }

    /**
     * Returns recommended groups.
     * @return the resulting group list
     */
    public List<Map<String, Object>> recommendedGroups() {
        return GroupDao.findRecommended(true, 12).stream()
                .map(this::group)
                .toList();
    }

    /**
     * Returns the current user's groups.
     * @param currentUser the current user
     * @return the resulting group list
     */
    public List<Map<String, Object>> myGroups(UserEntity currentUser) {
        return GroupDao.listByUserId(currentUser.getId()).stream()
                .map(this::group)
                .toList();
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
        currentUser.ifPresent(user -> members.add(member(user)));

        for (UserEntity user : UserDao.listRecentlyActive(18)) {
            if (members.stream().anyMatch(member -> user.getUsername().equalsIgnoreCase(String.valueOf(member.get("name"))))) {
                continue;
            }
            members.add(member(user));
            if (members.size() >= 18) {
                break;
            }
        }
        return members.stream().limit(18).toList();
    }

    private Map<String, Object> room(RoomEntity room) {
        return Map.of(
                "roomId", room.getId(),
                "name", room.getName(),
                "description", room.getDescription(),
                "owner", room.getOwnerName(),
                "ownerUrl", "/home/" + room.getOwnerName(),
                "occupancyClass", occupancyClass(room.getCurrentUsers())
        );
    }

    private Map<String, Object> group(GroupEntity group) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", group.getId());
        view.put("alias", group.getAlias());
        view.put("name", group.getName());
        view.put("badge", group.getBadge());
        view.put("description", group.getDescription());
        view.put("memberCount", GroupDao.countMembers(group.getId()));
        view.put("url", "/groups/" + group.getAlias());
        view.put("roomId", group.getRoomId());
        return view;
    }

    private static Map<String, Object> topic(String title, List<Integer> pages) {
        return Map.of(
                "title", title,
                "url", "/community",
                "pages", pages
        );
    }

    private Map<String, Object> member(UserEntity user) {
        Map<String, Object> currentUserView = new LinkedHashMap<>(userViewModelFactory.create(user));
        currentUserView.put("createdOn", formatActiveMemberDate(user.getCreatedAt()));
        currentUserView.put("status", user.isOnline() ? "online" : "offline");
        currentUserView.put("url", "/home/" + user.getUsername());
        return currentUserView;
    }

    private static String formatActiveMemberDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toLocalDateTime()
                .toLocalDate()
                .format(ACTIVE_MEMBER_DATE_FORMAT);
    }

    private static String occupancyClass(int currentUsers) {
        if (currentUsers >= 15) {
            return "room-occupancy-5";
        }
        if (currentUsers >= 12) {
            return "room-occupancy-4";
        }
        if (currentUsers >= 8) {
            return "room-occupancy-3";
        }
        if (currentUsers >= 4) {
            return "room-occupancy-2";
        }
        return "room-occupancy-1";
    }
}
