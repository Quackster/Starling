package org.starling.web.feature.me.quickmenu;

import io.javalin.http.Context;
import org.starling.storage.dao.GroupDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.entity.GroupEntity;
import org.starling.storage.entity.GroupMembershipEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.UserEntity;
import org.starling.web.feature.me.MeAccess;
import org.starling.web.feature.me.friends.WebMessengerDao;
import org.starling.web.feature.me.friends.WebMessengerFriend;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.site.SiteBranding;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Renders Lisbon-style signed-in quick menus.
 */
public final class QuickmenuController {

    private static final Comparator<WebMessengerFriend> FRIEND_ACTIVITY_ORDER =
            Comparator.comparingLong(WebMessengerFriend::lastOnlineEpoch)
                    .reversed()
                    .thenComparing(friend -> friend.username().toLowerCase())
                    .thenComparingInt(WebMessengerFriend::userId);

    private final TemplateRenderer templateRenderer;
    private final MeAccess meAccess;
    private final SiteBranding siteBranding;

    /**
     * Creates a new QuickmenuController.
     * @param templateRenderer the template renderer
     * @param meAccess the /me access helper
     * @param siteBranding the site branding helper
     */
    public QuickmenuController(TemplateRenderer templateRenderer, MeAccess meAccess, SiteBranding siteBranding) {
        this.templateRenderer = templateRenderer;
        this.meAccess = meAccess;
        this.siteBranding = siteBranding;
    }

    /**
     * Renders the friends quick menu.
     * @param context the request context
     */
    public void friends(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        List<WebMessengerFriend> friends = WebMessengerDao.listFriends(currentUser.get().getId());
        Map<String, Object> model = baseModel();
        model.put("onlineFriends", friends.stream()
                .filter(WebMessengerFriend::visibleOnline)
                .sorted(FRIEND_ACTIVITY_ORDER)
                .limit(10)
                .map(this::friendView)
                .toList());
        model.put("offlineFriends", friends.stream()
                .filter(friend -> !friend.visibleOnline())
                .sorted(FRIEND_ACTIVITY_ORDER)
                .limit(10)
                .map(this::friendView)
                .toList());
        context.html(templateRenderer.render("quickmenu/friends_all", model));
    }

    /**
     * Renders the groups quick menu.
     * @param context the request context
     */
    public void groups(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        UserEntity user = currentUser.get();
        Map<String, Object> model = baseModel();
        model.put("groups", GroupDao.listByUserId(user.getId()).stream()
                .map(group -> groupView(user, group))
                .toList());
        context.html(templateRenderer.render("quickmenu/groups", model));
    }

    /**
     * Renders the rooms quick menu.
     * @param context the request context
     */
    public void rooms(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> model = baseModel();
        model.put("rooms", RoomDao.findByOwner(currentUser.get().getUsername()).stream()
                .map(this::roomView)
                .toList());
        context.html(templateRenderer.render("quickmenu/rooms", model));
    }

    private Map<String, Object> baseModel() {
        return new LinkedHashMap<>(Map.of(
                "site", Map.of("sitePath", siteBranding.sitePath())
        ));
    }

    private Map<String, Object> friendView(WebMessengerFriend friend) {
        return Map.of(
                "userId", friend.userId(),
                "username", friend.username()
        );
    }

    private Map<String, Object> groupView(UserEntity currentUser, GroupEntity group) {
        GroupMembershipEntity membership = GroupDao.findMembership(currentUser.getId(), group.getId());
        boolean owned = group.getOwnerId() == currentUser.getId();
        boolean admin = membership != null && membership.getMemberRank() >= 2 && !owned;

        return Map.of(
                "id", group.getId(),
                "name", group.getName(),
                "url", "/groups/" + group.getAlias(),
                "roomId", group.getRoomId(),
                "owned", owned,
                "admin", admin,
                "favourite", currentUser.getFavouriteGroup() == group.getId()
        );
    }

    private Map<String, Object> roomView(RoomEntity room) {
        return Map.of(
                "id", room.getId(),
                "name", room.getName()
        );
    }
}
