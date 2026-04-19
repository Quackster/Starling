package org.starling.web.feature.tag.service;

import io.javalin.http.Context;
import org.starling.storage.dao.GroupDao;
import org.starling.storage.dao.PublicTagDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.GroupEntity;
import org.starling.storage.entity.UserEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TagDirectoryService {

    private static final int RESULTS_PER_PAGE = 10;

    private final UserTagService userTagService;

    /**
     * Creates a new TagDirectoryService.
     * @param userTagService the current-user tag service
     */
    public TagDirectoryService(UserTagService userTagService) {
        this.userTagService = userTagService;
    }

    /**
     * Returns the public tag cloud.
     * @param context the request context
     * @param currentUser the authenticated user, when present
     * @return the resulting tag cloud
     */
    public List<Map<String, Object>> tagCloud(Context context, Optional<UserEntity> currentUser) {
        Map<String, Long> counts = new LinkedHashMap<>();
        owners(context, currentUser).forEach(owner ->
                owner.tags().forEach(tag -> counts.merge(tag, 1L, Long::sum))
        );

        if (counts.isEmpty()) {
            return List.of();
        }

        long max = counts.values().stream().max(Long::compareTo).orElse(1L);
        long min = counts.values().stream().min(Long::compareTo).orElse(1L);
        long spread = Math.max(1L, max - min);

        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    long size = 100 + ((entry.getValue() - min) * 100 / spread);
                    return Map.<String, Object>of(
                            "name", entry.getKey(),
                            "url", "/tag/" + entry.getKey(),
                            "size", size
                    );
                })
                .toList();
    }

    /**
     * Returns the tag search page model.
     * @param context the request context
     * @param currentUser the authenticated user, when present
     * @param requestedTag the requested tag value
     * @param requestedPage the requested page number
     * @return the resulting search model
     */
    public Map<String, Object> search(Context context, Optional<UserEntity> currentUser, String requestedTag, int requestedPage) {
        String tag = userTagService.normalizeTag(requestedTag);
        int pageNumber = Math.max(1, requestedPage);

        List<Map<String, Object>> cloud = tagCloud(context, currentUser);
        List<Map<String, Object>> results = new ArrayList<>();
        for (TagOwner owner : owners(context, currentUser)) {
            if (!tag.isBlank() && owner.tags().contains(tag)) {
                results.add(searchResult(owner));
            }
        }

        int total = results.size();
        int pages = Math.max(1, (int) Math.ceil(total / (double) RESULTS_PER_PAGE));
        int currentPage = Math.min(pageNumber, pages);
        int offset = Math.max(0, (currentPage - 1) * RESULTS_PER_PAGE);
        int shownTo = Math.min(total, offset + RESULTS_PER_PAGE);
        List<Map<String, Object>> pageResults = offset >= total ? List.of() : results.subList(offset, shownTo);

        boolean hasTag = currentUser.isPresent() && userTagService.currentUserTags(context, currentUser.get()).contains(tag);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("query", tag);
        model.put("popularTags", cloud);
        model.put("results", pageResults);
        model.put("total", total);
        model.put("offset", total == 0 ? 0 : offset + 1);
        model.put("shownTo", shownTo);
        model.put("currentPage", currentPage);
        model.put("totalPages", pages);
        model.put("pageLinks", pageLinks(tag, currentPage, pages));
        model.put("showAddToMe", currentUser.isPresent() && !tag.isBlank() && !hasTag);
        return model;
    }

    /**
     * Returns the tag fight result.
     * @param context the request context
     * @param currentUser the authenticated user, when present
     * @param requestedTagOne the first tag
     * @param requestedTagTwo the second tag
     * @return the resulting fight model
     */
    public Map<String, Object> tagFight(Context context, Optional<UserEntity> currentUser, String requestedTagOne, String requestedTagTwo) {
        String tagOne = userTagService.normalizeTag(requestedTagOne);
        String tagTwo = userTagService.normalizeTag(requestedTagTwo);

        Map<String, Long> counts = new LinkedHashMap<>();
        owners(context, currentUser).forEach(owner ->
                owner.tags().forEach(tag -> counts.merge(tag, 1L, Long::sum))
        );

        long hitsOne = counts.getOrDefault(tagOne, 0L);
        long hitsTwo = counts.getOrDefault(tagTwo, 0L);
        int ending = hitsOne == hitsTwo ? 0 : (hitsOne > hitsTwo ? 2 : 1);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("tagOne", tagOne);
        model.put("tagTwo", tagTwo);
        model.put("tagOneHits", hitsOne);
        model.put("tagTwoHits", hitsTwo);
        model.put("ending", ending);
        model.put("winnerText", ending == 0 ? "It's a tie!" : "And the winner is:");
        return model;
    }

    /**
     * Returns the tag match result.
     * @param context the request context
     * @param currentUser the authenticated user
     * @param friendName the friend name
     * @return the resulting match model
     */
    public Map<String, Object> tagMatch(Context context, UserEntity currentUser, String friendName) {
        TagOwner friend = owners(context, Optional.of(currentUser)).stream()
                .filter(owner -> owner.type().equals("user"))
                .filter(owner -> owner.name().equalsIgnoreCase(friendName == null ? "" : friendName.trim()))
                .findFirst()
                .orElse(null);

        if (friend == null) {
            return Map.of("error", true, "message", "Friend not found");
        }

        Set<String> myTags = new LinkedHashSet<>(userTagService.currentUserTags(context, currentUser));
        Set<String> friendTags = new LinkedHashSet<>(friend.tags());
        Set<String> common = new LinkedHashSet<>(myTags);
        common.retainAll(friendTags);

        int denominator = Math.max(1, myTags.size());
        int percent = (int) Math.ceil(common.size() * 100.0 / denominator);

        return Map.of(
                "error", false,
                "percent", percent,
                "message", "You have a lot in common!"
        );
    }

    private List<Map<String, Object>> pageLinks(String tag, int currentPage, int totalPages) {
        if (tag.isBlank() || totalPages <= 1) {
            return List.of();
        }

        List<Map<String, Object>> links = new ArrayList<>();
        links.add(pageLink("First", "/tag/" + tag + "?pageNumber=1", currentPage == 1));
        for (int page = 1; page <= totalPages; page++) {
            links.add(pageLink(String.valueOf(page), "/tag/" + tag + "?pageNumber=" + page, currentPage == page));
        }
        links.add(pageLink("Last", "/tag/" + tag + "?pageNumber=" + totalPages, currentPage == totalPages));
        return links;
    }

    private Map<String, Object> pageLink(String label, String href, boolean current) {
        return Map.of("label", label, "href", href, "current", current);
    }

    private Map<String, Object> searchResult(TagOwner owner) {
        String image = owner.type().equals("user")
                ? "/habbo-imaging/avatarimage?figure=" + owner.figure() + "&size=s&direction=4&head_direction=4&gesture=sml"
                : "/habbo-imaging/badge/" + owner.badge() + ".gif";

        return Map.of(
                "type", owner.type(),
                "title", owner.name(),
                "description", owner.description(),
                "image", image,
                "url", owner.url(),
                "tags", owner.tags()
        );
    }

    private List<TagOwner> owners(Context context, Optional<UserEntity> currentUser) {
        List<TagOwner> owners = new ArrayList<>();
        Map<String, List<String>> tagsByOwner = loadTagsByOwner();

        for (UserEntity user : UserDao.listAll()) {
            List<String> tags = tagsByOwner.get(ownerKey("user", user.getId()));
            if (tags == null || tags.isEmpty()) {
                continue;
            }

            owners.add(new TagOwner(
                    "user",
                    user.getUsername(),
                    user.getFigure(),
                    "",
                    user.getMotto() == null || user.getMotto().isBlank() ? "Exploring the hotel." : user.getMotto(),
                    tags,
                    "/home/" + user.getUsername()
            ));
        }

        for (GroupEntity group : GroupDao.listAll()) {
            List<String> tags = tagsByOwner.get(ownerKey("group", group.getId()));
            if (tags == null || tags.isEmpty()) {
                continue;
            }

            owners.add(new TagOwner(
                    "group",
                    group.getName(),
                    "",
                    group.getBadge(),
                    group.getDescription(),
                    tags,
                    "/groups/" + group.getAlias()
            ));
        }

        return owners.stream()
                .sorted(Comparator.comparing(TagOwner::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<String, List<String>> loadTagsByOwner() {
        Map<String, List<String>> tagsByOwner = new HashMap<>();
        for (var tag : PublicTagDao.listAll()) {
            tagsByOwner.computeIfAbsent(ownerKey(tag.getType(), tag.getOwnerId()), ignored -> new ArrayList<>())
                    .add(tag.getTag());
        }
        return tagsByOwner;
    }

    private String ownerKey(String type, int ownerId) {
        return type + ":" + ownerId;
    }

    private record TagOwner(
            String type,
            String name,
            String figure,
            String badge,
            String description,
            List<String> tags,
            String url
    ) {
    }
}
