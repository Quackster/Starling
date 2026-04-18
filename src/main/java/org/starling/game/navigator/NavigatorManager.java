package org.starling.game.navigator;

import org.starling.storage.dao.NavigatorDao;
import org.starling.storage.entity.NavigatorCategoryEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages navigator categories. Loads from DB on startup and caches.
 */
public final class NavigatorManager {

    private static final NavigatorManager INSTANCE = new NavigatorManager();
    private static final Comparator<NavigatorCategoryEntity> CATEGORY_ORDER =
            Comparator.comparingInt(NavigatorCategoryEntity::getOrderId)
                    .thenComparingInt(NavigatorCategoryEntity::getId);

    private final Map<Integer, NavigatorCategoryEntity> categories = new ConcurrentHashMap<>();
    private final Map<Integer, List<NavigatorCategoryEntity>> childrenByParent = new ConcurrentHashMap<>();

    /**
     * Creates a new NavigatorManager.
     */
    private NavigatorManager() {}

    /**
     * Returns the instance.
     * @return the instance
     */
    public static NavigatorManager getInstance() {
        return INSTANCE;
    }

    /**
     * Loads.
     */
    public void load() {
        List<NavigatorCategoryEntity> all = NavigatorDao.findAll();
        categories.clear();
        childrenByParent.clear();

        all.forEach(category -> categories.put(category.getId(), category));
        all.forEach(category -> childrenByParent
                .computeIfAbsent(category.getParentId(), ignored -> new ArrayList<>())
                .add(category));

        for (List<NavigatorCategoryEntity> children : childrenByParent.values()) {
            children.sort(CATEGORY_ORDER);
        }
    }

    /**
     * Gets category.
     * @param id the id value
     * @return the result of this operation
     */
    public NavigatorCategoryEntity getCategory(int id) {
        return categories.get(id);
    }

    /**
     * Gets children.
     * @param parentId the parent id value
     * @return the result of this operation
     */
    public List<NavigatorCategoryEntity> getChildren(int parentId) {
        return childrenByParent.getOrDefault(parentId, Collections.emptyList());
    }

    /** Get categories that hold user-created flats (non-public, leaf categories). */
    public List<NavigatorCategoryEntity> getFlatCategories() {
        return categories.values().stream()
                .filter(NavigatorCategoryEntity::isFlatCategory)
                .sorted(CATEGORY_ORDER)
                .toList();
    }

    /**
     * Gets assignable flat categories.
     * @param rank the rank value
     * @return the result of this operation
     */
    public List<NavigatorCategoryEntity> getAssignableFlatCategories(int rank) {
        return getFlatCategories().stream()
                .filter(category -> rank >= category.getMinRoleAccess())
                .filter(category -> rank >= category.getMinRoleSetFlatCat())
                .toList();
    }

    /**
     * Gets accessible children.
     * @param parentId the parent id value
     * @param rank the rank value
     * @return the result of this operation
     */
    public List<NavigatorCategoryEntity> getAccessibleChildren(int parentId, int rank) {
        List<NavigatorCategoryEntity> children = getChildren(parentId);
        if (children.isEmpty()) {
            return children;
        }

        return children.stream()
                .filter(child -> rank >= child.getMinRoleAccess())
                .toList();
    }

    /**
     * Gets parent chain.
     * @param categoryId the category id value
     * @return the result of this operation
     */
    public List<NavigatorCategoryEntity> getParentChain(int categoryId) {
        NavigatorCategoryEntity current = getCategory(categoryId);
        if (current == null) {
            return List.of();
        }

        List<NavigatorCategoryEntity> chain = new ArrayList<>();
        while (current != null && current.getParentId() > 0) {
            current = getCategory(current.getParentId());
            if (current != null) {
                chain.add(current);
            }
        }
        return chain;
    }

    /**
     * Returns the category count.
     * @return the category count
     */
    public int getCategoryCount() {
        return categories.size();
    }
}
