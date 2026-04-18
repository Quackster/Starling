package org.starling.web.view;

import org.starling.storage.entity.UserEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public final class UserViewModelFactory {

    /**
     * Creates a public user view model.
     * @param user the user value
     * @return the resulting view model
     */
    public Map<String, Object> create(UserEntity user) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("name", user.getUsername());
        view.put("figure", user.getFigure());
        view.put("motto", valueOrDefault(user.getMotto(), ""));
        view.put("email", valueOrDefault(user.getEmail(), ""));
        view.put("credits", user.getCredits());
        view.put("pixels", user.getPixels());
        view.put("rankId", user.getRank());
        view.put("lastOnline", formatFriendlyDate(user.getLastOnline()));
        view.put("memberSince", formatFriendlyDate(user.getCreatedAt()));
        view.put("clubActive", user.hasClubSubscription());
        view.put("clubDays", user.hasClubSubscription()
                ? Math.max(0, (int) ((user.getClubExpiration() - Instant.now().getEpochSecond()) / 86400))
                : 0);
        return view;
    }

    private static String formatFriendlyDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? fallback : normalized;
    }
}
