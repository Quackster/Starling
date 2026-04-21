package org.oldskooler.vibe.web.cms.page;

import org.oldskooler.vibe.storage.entity.UserEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public final class CmsPageAccessControl {

    /**
     * Creates a new CmsPageAccessControl.
     */
    private CmsPageAccessControl() {}

    /**
     * Returns whether the current request may view a page.
     * @param visibleToGuests whether guests may view the page
     * @param allowedRanksCsv the allowed rank csv
     * @param currentUser the current user, when present
     * @return true when the page may be viewed
     */
    public static boolean canView(boolean visibleToGuests, String allowedRanksCsv, Optional<UserEntity> currentUser) {
        if (currentUser.map(UserEntity::isAdmin).orElse(false)) {
            return true;
        }

        if (visibleToGuests) {
            return true;
        }

        if (currentUser.isEmpty()) {
            return false;
        }

        List<Integer> allowedRanks = allowedRanks(allowedRanksCsv);
        if (allowedRanks.isEmpty()) {
            return true;
        }

        return allowedRanks.contains(currentUser.get().getRank());
    }

    /**
     * Returns whether a page requires login.
     * @param visibleToGuests whether guests may view the page
     * @return true when login is required
     */
    public static boolean requiresLogin(boolean visibleToGuests) {
        return !visibleToGuests;
    }

    /**
     * Parses allowed ranks from csv.
     * @param allowedRanksCsv the csv value
     * @return the parsed ranks
     */
    public static List<Integer> allowedRanks(String allowedRanksCsv) {
        LinkedHashSet<Integer> ranks = new LinkedHashSet<>();
        String normalized = allowedRanksCsv == null ? "" : allowedRanksCsv.trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        for (String part : normalized.split(",")) {
            try {
                int rank = Integer.parseInt(part.trim());
                if (rank > 0) {
                    ranks.add(rank);
                }
            } catch (Exception ignored) {
            }
        }

        return new ArrayList<>(ranks);
    }

    /**
     * Converts allowed ranks into csv.
     * @param ranks the ranks
     * @return the csv value
     */
    public static String toCsv(List<Integer> ranks) {
        return ranks.stream()
                .filter(rank -> rank != null && rank > 0)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    /**
     * Returns a user-facing access summary.
     * @param visibleToGuests whether guests may view the page
     * @param allowedRanksCsv the allowed ranks csv
     * @return the summary
     */
    public static String summary(boolean visibleToGuests, String allowedRanksCsv) {
        if (visibleToGuests) {
            return "Guests and signed-in users";
        }

        List<Integer> allowedRanks = allowedRanks(allowedRanksCsv);
        if (allowedRanks.isEmpty()) {
            return "Signed-in users";
        }

        return "Ranks " + allowedRanks.stream()
                .map(String::valueOf)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }
}
