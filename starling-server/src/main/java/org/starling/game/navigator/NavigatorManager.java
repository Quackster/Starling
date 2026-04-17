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
public class NavigatorManager {

    private static NavigatorManager instance;
    private static final Comparator<NavigatorCategoryEntity> CATEGORY_ORDER =
            Comparator.comparingInt(NavigatorCategoryEntity::getOrderId)
                    .thenComparingInt(NavigatorCategoryEntity::getId);

    private final Map<Integer, NavigatorCategoryEntity> categories = new ConcurrentHashMap<>();
    private final Map<Integer, List<NavigatorCategoryEntity>> childrenByParent = new ConcurrentHashMap<>();

    public static NavigatorManager getInstance() {
        if (instance == null) {
            instance = new NavigatorManager();
        }
        return instance;
    }

    public void load() {
        List<NavigatorCategoryEntity> all = NavigatorDao.findAll();
        categories.clear();
        childrenByParent.clear();

        for (NavigatorCategoryEntity cat : all) {
            categories.put(cat.getId(), cat);
        }

        // Build parent->children index
        for (NavigatorCategoryEntity cat : all) {
            childrenByParent.computeIfAbsent(cat.getParentId(), k -> new java.util.ArrayList<>()).add(cat);
        }

        for (List<NavigatorCategoryEntity> children : childrenByParent.values()) {
            children.sort(CATEGORY_ORDER);
        }
    }

    public NavigatorCategoryEntity getCategory(int id) {
        return categories.get(id);
    }

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

    public List<NavigatorCategoryEntity> getAssignableFlatCategories(int rank) {
        return getFlatCategories().stream()
                .filter(category -> rank >= category.getMinRoleAccess())
                .filter(category -> rank >= category.getMinRoleSetFlatCat())
                .toList();
    }

    public List<NavigatorCategoryEntity> getAccessibleChildren(int parentId, int rank) {
        List<NavigatorCategoryEntity> children = getChildren(parentId);
        if (children.isEmpty()) {
            return children;
        }

        List<NavigatorCategoryEntity> filtered = new ArrayList<>();
        for (NavigatorCategoryEntity child : children) {
            if (rank >= child.getMinRoleAccess()) {
                filtered.add(child);
            }
        }
        return filtered;
    }

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

    public int getCategoryCount() {
        return categories.size();
    }
}
