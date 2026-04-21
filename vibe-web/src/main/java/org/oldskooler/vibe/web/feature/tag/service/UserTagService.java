package org.oldskooler.vibe.web.feature.tag.service;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.dao.PublicTagDao;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.util.Slugifier;

import java.util.List;
import java.util.Locale;

public final class UserTagService {

    private static final int MAX_TAGS_PER_USER = 20;
    private static final List<String> TAG_QUESTIONS = List.of(
            "What is your favourite TV show?",
            "Who is your favourite actor?",
            "Who is your favourite actress?",
            "Do you have a nickname?",
            "What is your favorite song?",
            "How do you describe yourself?",
            "What is your favorite band?",
            "What is your favorite comic?",
            "What are you like?",
            "What is your favorite food?",
            "What sport you play?",
            "Who was your first love?",
            "What is your favorite movie?",
            "Cats, dogs, or something more exotic?",
            "What is your favorite color?",
            "Do you have a favorite staff member?"
    );

    /**
     * Returns the current user's tags.
     * @param context the request context
     * @param user the authenticated user
     * @return the resulting tag list
     */
    public List<String> currentUserTags(Context context, UserEntity user) {
        return PublicTagDao.listByOwner("user", user.getId()).stream()
                .map(tag -> tag.getTag().toLowerCase(Locale.ROOT))
                .toList();
    }

    /**
     * Returns the random tag question shown in the add-tag form.
     * @return the resulting question
     */
    public String tagQuestion() {
        return TAG_QUESTIONS.get((int) (System.currentTimeMillis() % TAG_QUESTIONS.size()));
    }

    /**
     * Adds a tag for the current user.
     * @param context the request context
     * @param user the authenticated user
     * @param requestedTag the tag value
     * @return the result code expected by the Lisbon scripts
     */
    public String addTag(Context context, UserEntity user, String requestedTag) {
        String tag = normalizeTag(requestedTag);
        if (tag.isBlank()) {
            return "invalidtag";
        }

        List<String> tags = currentUserTags(context, user);
        if (tags.contains(tag)) {
            return "valid";
        }
        if (tags.size() >= MAX_TAGS_PER_USER) {
            return "taglimit";
        }

        PublicTagDao.addTag("user", user.getId(), tag);
        return "valid";
    }

    /**
     * Removes a tag for the current user.
     * @param context the request context
     * @param user the authenticated user
     * @param requestedTag the tag value
     */
    public void removeTag(Context context, UserEntity user, String requestedTag) {
        String tag = normalizeTag(requestedTag);
        PublicTagDao.removeTag("user", user.getId(), tag);
    }

    /**
     * Normalizes a raw tag value into the hotel's public tag format.
     * @param requestedTag the raw tag
     * @return the normalized tag
     */
    public String normalizeTag(String requestedTag) {
        String normalized = Slugifier.slugify(requestedTag);
        if (normalized.length() > 20) {
            normalized = normalized.substring(0, 20);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
