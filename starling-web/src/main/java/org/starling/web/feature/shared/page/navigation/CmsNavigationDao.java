package org.starling.web.feature.shared.page.navigation;

import org.oldskooler.entity4j.DbContext;
import org.starling.storage.EntityContext;

import java.util.ArrayList;
import java.util.List;

public final class CmsNavigationDao {

    /**
     * Creates a new CmsNavigationDao.
     */
    private CmsNavigationDao() {}

    /**
     * Counts stored navigation links.
     * @return the link count
     */
    public static int countLinks() {
        return EntityContext.withContext(context -> Math.toIntExact(context.from(CmsNavigationLinkEntity.class).count()));
    }

    /**
     * Counts stored navigation buttons.
     * @return the button count
     */
    public static int countButtons() {
        return EntityContext.withContext(context -> Math.toIntExact(context.from(CmsNavigationButtonEntity.class).count()));
    }

    /**
     * Lists all navigation links in menu order.
     * @return the links
     */
    public static List<CmsNavigationLinkDraft> listLinks() {
        return EntityContext.withContext(context -> context.from(CmsNavigationLinkEntity.class)
                .orderBy(order -> order
                        .col(CmsNavigationLinkEntity::getMenuType).asc()
                        .col(CmsNavigationLinkEntity::getGroupKey).asc()
                        .col(CmsNavigationLinkEntity::getSortOrder).asc()
                        .col(CmsNavigationLinkEntity::getId).asc())
                .toList()
                .stream()
                .map(CmsNavigationDao::mapLink)
                .toList());
    }

    /**
     * Lists all navigation buttons in menu order.
     * @return the buttons
     */
    public static List<CmsNavigationButtonDraft> listButtons() {
        return EntityContext.withContext(context -> context.from(CmsNavigationButtonEntity.class)
                .orderBy(order -> order
                        .col(CmsNavigationButtonEntity::getSortOrder).asc()
                        .col(CmsNavigationButtonEntity::getId).asc())
                .toList()
                .stream()
                .map(CmsNavigationDao::mapButton)
                .toList());
    }

    /**
     * Replaces all stored links and buttons.
     * @param mainLinks the main links
     * @param subLinks the sub links
     * @param buttons the buttons
     */
    public static void replaceAll(
            List<CmsNavigationLinkDraft> mainLinks,
            List<CmsNavigationLinkDraft> subLinks,
            List<CmsNavigationButtonDraft> buttons
    ) {
        EntityContext.inTransaction(context -> {
            deleteAllLinks(context);
            deleteAllButtons(context);

            insertLinks(context, combine(mainLinks, subLinks));
            insertButtons(context, buttons);
            return null;
        });
    }

    /**
     * Replaces all stored links.
     * @param mainLinks the main links
     * @param subLinks the sub links
     */
    public static void replaceLinks(List<CmsNavigationLinkDraft> mainLinks, List<CmsNavigationLinkDraft> subLinks) {
        EntityContext.inTransaction(context -> {
            deleteAllLinks(context);
            insertLinks(context, combine(mainLinks, subLinks));
            return null;
        });
    }

    /**
     * Replaces all stored buttons.
     * @param buttons the buttons
     */
    public static void replaceButtons(List<CmsNavigationButtonDraft> buttons) {
        EntityContext.inTransaction(context -> {
            deleteAllButtons(context);
            insertButtons(context, buttons);
            return null;
        });
    }

    private static void deleteAllLinks(DbContext context) {
        context.from(CmsNavigationLinkEntity.class)
                .filter(filter -> filter.notEquals(CmsNavigationLinkEntity::getId, 0))
                .delete();
    }

    private static void deleteAllButtons(DbContext context) {
        context.from(CmsNavigationButtonEntity.class)
                .filter(filter -> filter.notEquals(CmsNavigationButtonEntity::getId, 0))
                .delete();
    }

    private static void insertLinks(DbContext context, List<CmsNavigationLinkDraft> links) {
        for (CmsNavigationLinkDraft link : links) {
            CmsNavigationLinkEntity entity = new CmsNavigationLinkEntity();
            entity.setMenuType(link.menuType());
            entity.setGroupKey(link.groupKey());
            entity.setLinkKey(link.key());
            entity.setLabel(link.label());
            entity.setHref(link.href());
            entity.setSelectedKeys(NavigationSelectionCodec.toCsv(link.selectedKeys()));
            entity.setVisibleWhenLoggedIn(link.visibleWhenLoggedIn() ? 1 : 0);
            entity.setVisibleWhenLoggedOut(link.visibleWhenLoggedOut() ? 1 : 0);
            entity.setCssId(link.cssId());
            entity.setCssClass(link.cssClass());
            entity.setMinimumRank(link.minimumRank());
            entity.setRequiresAdminRole(link.requiresAdminRole() ? 1 : 0);
            entity.setRequiredPermission(link.requiredPermission());
            entity.setSortOrder(link.sortOrder());
            context.insert(entity);
        }
    }

    private static void insertButtons(DbContext context, List<CmsNavigationButtonDraft> buttons) {
        for (CmsNavigationButtonDraft button : buttons) {
            CmsNavigationButtonEntity entity = new CmsNavigationButtonEntity();
            entity.setButtonKey(button.key());
            entity.setLabel(button.label());
            entity.setHref(button.href());
            entity.setVisibleWhenLoggedIn(button.visibleWhenLoggedIn() ? 1 : 0);
            entity.setVisibleWhenLoggedOut(button.visibleWhenLoggedOut() ? 1 : 0);
            entity.setCssId(button.cssId());
            entity.setCssClass(button.cssClass());
            entity.setButtonColor(button.buttonColor());
            entity.setTarget(button.target());
            entity.setOnclick(button.onclick());
            entity.setSortOrder(button.sortOrder());
            context.insert(entity);
        }
    }

    private static CmsNavigationLinkDraft mapLink(CmsNavigationLinkEntity entity) {
        return new CmsNavigationLinkDraft(
                entity.getMenuType(),
                safeValue(entity.getGroupKey()),
                safeValue(entity.getLinkKey()),
                safeValue(entity.getLabel()),
                safeValue(entity.getHref()),
                NavigationSelectionCodec.values(entity.getSelectedKeys()),
                entity.getVisibleWhenLoggedIn() > 0,
                entity.getVisibleWhenLoggedOut() > 0,
                safeValue(entity.getCssId()),
                safeValue(entity.getCssClass()),
                entity.getMinimumRank(),
                entity.getRequiresAdminRole() > 0,
                safeValue(entity.getRequiredPermission()),
                entity.getSortOrder()
        );
    }

    private static CmsNavigationButtonDraft mapButton(CmsNavigationButtonEntity entity) {
        return new CmsNavigationButtonDraft(
                safeValue(entity.getButtonKey()),
                safeValue(entity.getLabel()),
                safeValue(entity.getHref()),
                entity.getVisibleWhenLoggedIn() > 0,
                entity.getVisibleWhenLoggedOut() > 0,
                safeValue(entity.getCssId()),
                safeValue(entity.getCssClass()),
                safeValue(entity.getButtonColor()),
                safeValue(entity.getTarget()),
                safeValue(entity.getOnclick()),
                entity.getSortOrder()
        );
    }

    private static List<CmsNavigationLinkDraft> combine(List<CmsNavigationLinkDraft> mainLinks, List<CmsNavigationLinkDraft> subLinks) {
        List<CmsNavigationLinkDraft> combined = new ArrayList<>(mainLinks);
        combined.addAll(subLinks);
        return combined;
    }

    private static String safeValue(String value) {
        return value == null ? "" : value;
    }
}
