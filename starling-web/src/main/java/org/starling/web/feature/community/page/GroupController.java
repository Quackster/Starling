package org.starling.web.feature.community.page;

import io.javalin.http.Context;
import org.starling.storage.dao.GroupDao;
import org.starling.storage.dao.PublicTagDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.GroupEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.UserEntity;
import org.starling.web.feature.shared.page.PublicPageModelFactory;
import org.starling.web.render.TemplateRenderer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GroupController {

    private final TemplateRenderer templateRenderer;
    private final PublicPageModelFactory publicPageModelFactory;

    /**
     * Creates a new GroupController.
     * @param templateRenderer the template renderer
     * @param publicPageModelFactory the public page model factory
     */
    public GroupController(TemplateRenderer templateRenderer, PublicPageModelFactory publicPageModelFactory) {
        this.templateRenderer = templateRenderer;
        this.publicPageModelFactory = publicPageModelFactory;
    }

    /**
     * Renders the public group page.
     * @param context the request context
     */
    public void detail(Context context) {
        GroupEntity group = GroupDao.findByAlias(context.pathParam("alias"));
        if (group == null) {
            context.status(404).result("Group not found");
            return;
        }

        UserEntity owner = UserDao.findById(group.getOwnerId());
        RoomEntity room = group.getRoomId() > 0 ? RoomDao.findById(group.getRoomId()) : null;
        List<Map<String, Object>> members = GroupDao.listMemberIds(group.getId(), 24).stream()
                .map(UserDao::findById)
                .filter(Objects::nonNull)
                .map(this::memberView)
                .toList();
        List<String> tags = PublicTagDao.listByOwner("group", group.getId()).stream()
                .map(tag -> tag.getTag())
                .toList();

        Map<String, Object> model = publicPageModelFactory.create(context, "community", "community");
        model.put("group", groupView(group, owner, room, members, tags));
        context.html(templateRenderer.render("group", model));
    }

    private Map<String, Object> groupView(
            GroupEntity group,
            UserEntity owner,
            RoomEntity room,
            List<Map<String, Object>> members,
            List<String> tags
    ) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", group.getId());
        view.put("alias", group.getAlias());
        view.put("name", group.getName());
        view.put("badge", group.getBadge());
        view.put("description", group.getDescription());
        view.put("memberCount", GroupDao.countMembers(group.getId()));
        view.put("members", members);
        view.put("tags", tags);
        view.put("ownerName", owner != null ? owner.getUsername() : "");
        view.put("ownerUrl", owner != null ? "/home/" + owner.getUsername() : "");
        view.put("roomId", room != null ? room.getId() : 0);
        view.put("roomName", room != null ? room.getName() : "");
        return view;
    }

    private Map<String, Object> memberView(UserEntity user) {
        return Map.of(
                "name", user.getUsername(),
                "figure", user.getFigure(),
                "url", "/home/" + user.getUsername()
        );
    }
}
