package org.starling.web.feature.me.mail;

import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MinimailRecipientService {

    private static final int MAX_RECIPIENTS = 50;

    /**
     * Returns recipient suggestions for the minimail autocomplete.
     * @return the recipient suggestions
     */
    public List<Map<String, Object>> recipientOptions() {
        return UserDao.listAll().stream()
                .map(user -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("id", user.getId());
                    option.put("name", user.getUsername());
                    return option;
                })
                .toList();
    }

    /**
     * Resolves comma-separated usernames into minimail recipients.
     * @param recipientsRaw the raw usernames
     * @return the resolved recipients
     */
    public List<UserEntity> parseRecipients(String recipientsRaw) {
        String normalized = recipientsRaw == null ? "" : recipientsRaw.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Enter at least one username.");
        }

        String[] parts = normalized.split(",");
        Set<String> usernames = new LinkedHashSet<>();
        for (String part : parts) {
            String username = part == null ? "" : part.trim();
            if (!username.isBlank()) {
                usernames.add(username);
            }
        }

        if (usernames.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one username.");
        }
        if (usernames.size() > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("You can send to up to " + MAX_RECIPIENTS + " recipients at once.");
        }

        List<UserEntity> recipients = new ArrayList<>();
        for (String username : usernames) {
            UserEntity recipient = UserDao.findByUsername(username);
            if (recipient == null) {
                throw new IllegalArgumentException("Could not find the user \"" + username + "\".");
            }
            recipients.add(recipient);
        }
        return recipients;
    }

    /**
     * Resolves comma-separated user ids into minimail recipients.
     * @param recipientIdsRaw the raw recipient ids
     * @return the resolved recipients
     */
    public List<UserEntity> parseRecipientIds(String recipientIdsRaw) {
        String normalized = recipientIdsRaw == null ? "" : recipientIdsRaw.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Choose at least one recipient.");
        }

        String[] parts = normalized.split(",");
        Set<Integer> recipientIds = new LinkedHashSet<>();
        for (String part : parts) {
            int id = parseRecipientId(part);
            if (id > 0) {
                recipientIds.add(id);
            }
        }

        if (recipientIds.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one recipient.");
        }
        if (recipientIds.size() > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("You can send to up to " + MAX_RECIPIENTS + " recipients at once.");
        }

        List<UserEntity> recipients = new ArrayList<>();
        for (Integer recipientId : recipientIds) {
            UserEntity recipient = UserDao.findById(recipientId);
            if (recipient == null) {
                throw new IllegalArgumentException("One or more recipients could not be found.");
            }
            recipients.add(recipient);
        }
        return recipients;
    }

    private int parseRecipientId(String rawValue) {
        try {
            return Integer.parseInt(rawValue == null ? "" : rawValue.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }
}
