package org.oldskooler.vibe.permission;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class RankPermissionCatalog {

    public static final int MIN_RANK = 1;
    public static final int MAX_RANK = 7;
    public static final int GOD_MODE_RANK = 7;

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(
                    RankPermissionKeys.HOUSEKEEPING_ACCESS,
                    "Access Housekeeping",
                    "Allows admins to open /admin and see the dashboard.",
                    "Housekeeping",
                    6
            ),
            new Definition(
                    RankPermissionKeys.HOUSEKEEPING_PAGES,
                    "Manage Pages",
                    "Create, edit, publish, and unpublish CMS pages.",
                    "Housekeeping",
                    6
            ),
            new Definition(
                    RankPermissionKeys.HOUSEKEEPING_NAVIGATION,
                    "Manage Navigation",
                    "Edit the public navigation menus and hotel action buttons.",
                    "Housekeeping",
                    6
            ),
            new Definition(
                    RankPermissionKeys.HOUSEKEEPING_ARTICLES,
                    "Manage News",
                    "Create, edit, publish, and unpublish news articles.",
                    "Housekeeping",
                    6
            ),
            new Definition(
                    RankPermissionKeys.HOUSEKEEPING_CAMPAIGNS,
                    "Manage Campaigns",
                    "Create, edit, hide, and delete hot campaigns.",
                    "Housekeeping",
                    6
            ),
            new Definition(
                    RankPermissionKeys.HOUSEKEEPING_USERS,
                    "Manage Users",
                    "Update hotel user ranks and housekeeping roles.",
                    "Housekeeping",
                    7
            ),
            new Definition(
                    RankPermissionKeys.HOUSEKEEPING_PERMISSIONS,
                    "Manage Permissions",
                    "Edit the rank permission matrix itself.",
                    "Housekeeping",
                    7
            ),
            new Definition(
                    RankPermissionKeys.HOUSEKEEPING_SETTINGS,
                    "Manage Settings",
                    "Edit generic web, client, and security settings.",
                    "Housekeeping",
                    7
            ),
            new Definition(
                    RankPermissionKeys.FUSE_LOGIN,
                    "Login",
                    "Lets the client finish the normal hotel login flow.",
                    "Fuse Rights",
                    1
            ),
            new Definition(
                    RankPermissionKeys.FUSE_BUY_CREDITS,
                    "Buy Credits",
                    "Shows the classic buy-credits right in the client.",
                    "Fuse Rights",
                    1
            ),
            new Definition(
                    RankPermissionKeys.FUSE_TRADE,
                    "Trade",
                    "Allows the classic trading fuse right.",
                    "Fuse Rights",
                    1
            ),
            new Definition(
                    RankPermissionKeys.FUSE_ROOM_QUEUE_DEFAULT,
                    "Default Queue",
                    "Allows the default room queue right.",
                    "Fuse Rights",
                    1
            ),
            new Definition(
                    RankPermissionKeys.FUSE_ALERT,
                    "Alert",
                    "Grants the classic alert right.",
                    "Fuse Rights",
                    5
            ),
            new Definition(
                    RankPermissionKeys.FUSE_ROOM_KICK,
                    "Kick",
                    "Grants the room kick moderation right.",
                    "Fuse Rights",
                    5
            ),
            new Definition(
                    RankPermissionKeys.FUSE_ROOM_BAN,
                    "Ban",
                    "Grants the room ban moderation right.",
                    "Fuse Rights",
                    6
            ),
            new Definition(
                    RankPermissionKeys.FUSE_ENTER_ANY_ROOM,
                    "Enter Any Room",
                    "Allows staff to enter rooms without normal access checks.",
                    "Fuse Rights",
                    6
            ),
            new Definition(
                    RankPermissionKeys.FUSE_PICK_UP_ANY_FURNI,
                    "Pick Up Any Furni",
                    "Allows staff to move furniture without owner rights.",
                    "Fuse Rights",
                    6
            ),
            new Definition(
                    RankPermissionKeys.FUSE_IGNORE_ROOM_RIGHTS,
                    "Ignore Room Rights",
                    "Lets staff bypass room-right restrictions.",
                    "Fuse Rights",
                    6
            )
    );

    /**
     * Creates a new RankPermissionCatalog.
     */
    private RankPermissionCatalog() {}

    /**
     * Returns all permission definitions.
     * @return the permission definitions
     */
    public static List<Definition> definitions() {
        return DEFINITIONS;
    }

    /**
     * Returns the permission definitions grouped by category.
     * @return the grouped definitions
     */
    public static Map<String, List<Definition>> groupedDefinitions() {
        return DEFINITIONS.stream()
                .collect(Collectors.groupingBy(Definition::category, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Returns fuse-right definitions only.
     * @return the fuse-right definitions
     */
    public static List<Definition> fuseRights() {
        return DEFINITIONS.stream()
                .filter(definition -> definition.key().startsWith("fuse_"))
                .toList();
    }

    /**
     * Finds a definition by key.
     * @param key the permission key
     * @return the resulting definition
     */
    public static Optional<Definition> find(String key) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.key().equals(key))
                .findFirst();
    }

    public record Definition(
            String key,
            String label,
            String description,
            String category,
            int defaultMinimumRank
    ) {
    }
}
