package org.starling.web.feature.me.content;

import org.starling.storage.entity.UserEntity;
import org.starling.web.feature.me.friends.WebMessengerDao;
import org.starling.web.feature.me.friends.WebMessengerFriend;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MePageContentFactory {

    /**
     * Builds the personal-info widget state for the /me page.
     * @param user the current user
     * @return the widget model
     */
    public Map<String, Object> personalInfo(UserEntity user) {
        Map<String, Object> model = new LinkedHashMap<>();
        LocalDate joinDate = user.getCreatedAt() == null
                ? null
                : user.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        boolean hasBirthday = joinDate != null
                && joinDate.getYear() != today.getYear()
                && joinDate.getMonth() == today.getMonth()
                && joinDate.getDayOfMonth() == today.getDayOfMonth();
        int birthdayAge = hasBirthday ? Period.between(joinDate, today).getYears() : 0;
        List<Map<String, Object>> visibleOnlineFriends = WebMessengerDao.listFriends(user.getId()).stream()
                .filter(WebMessengerFriend::visibleOnline)
                .map(friend -> Map.<String, Object>of(
                        "userId", friend.userId(),
                        "username", friend.username()
                ))
                .toList();

        model.put("feedFriendRequests", WebMessengerDao.countRequests(user.getId()));
        model.put("feedFriendsOnline", visibleOnlineFriends);
        model.put("hasBirthday", hasBirthday);
        model.put("birthdayAge", birthdayAge);
        model.put("birthdayPrefix", ordinalSuffix(birthdayAge));
        model.put("showGuidesPlaceholder", true);
        model.put("guidesPlaceholderHref", "/guides");
        model.put("guidesPlaceholderText", "Habbo Guides are coming soon.");
        return model;
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

    private static String ordinalSuffix(int value) {
        int tens = value % 100;
        if (tens >= 11 && tens <= 13) {
            return "th";
        }

        return switch (value % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
