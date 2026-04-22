package org.oldskooler.vibe.storage.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledPublicSpaceCatalogTest {

    @Test
    void navigatorCategoriesReserveRootIdsAndRemainUnique() {
        List<BundledPublicSpaceCatalog.NavigatorCategorySeed> categories = BundledPublicSpaceCatalog.load()
                .navigatorCategories();

        Set<Integer> categoryIds = new HashSet<>();
        for (BundledPublicSpaceCatalog.NavigatorCategorySeed category : categories) {
            assertTrue(categoryIds.add(category.id()), "Duplicate navigator category id " + category.id());
        }

        assertTrue(categories.stream().anyMatch(category -> category.id() == 1
                && category.parentId() == 0
                && BundledPublicSpaceCatalog.ROOT_PUBLIC_CATEGORY_NAME.equals(category.name())));
        assertTrue(categories.stream().anyMatch(category -> category.id() == 2
                && category.parentId() == 0
                && BundledPublicSpaceCatalog.ROOT_PRIVATE_CATEGORY_NAME.equals(category.name())));

        List<BundledPublicSpaceCatalog.NavigatorCategorySeed> noCategoryMatches = categories.stream()
                .filter(category -> "No category".equals(category.name()))
                .toList();
        assertEquals(1, noCategoryMatches.size());
        assertFalse(noCategoryMatches.get(0).id() == 1 || noCategoryMatches.get(0).id() == 2);
        assertEquals(2, noCategoryMatches.get(0).parentId());
    }
}
