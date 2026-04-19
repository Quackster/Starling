package org.starling.web.admin.user;

import io.javalin.http.Context;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.util.Htmx;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AdminUsersController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;

    /**
     * Creates a new AdminUsersController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     */
    public AdminUsersController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
    }

    /**
     * Renders the user index.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/users");
        model.put("users", UserDao.listAll().stream().map(this::userSummary).toList());
        context.html(templateRenderer.render("admin-layout", "admin/users/index", model));
    }

    /**
     * Renders an existing user editor.
     * @param context the request context
     */
    public void edit(Context context) {
        UserEntity user = requireUser(Integer.parseInt(context.pathParam("id")));
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/users");
        model.put("user", userEditor(user));
        context.html(templateRenderer.render("admin-layout", "admin/users/form", model));
    }

    /**
     * Updates a hotel user's rank and admin role.
     * @param context the request context
     */
    public void update(Context context) {
        UserEntity user = requireUser(Integer.parseInt(context.pathParam("id")));
        AdminUserRequest request = AdminUserRequest.from(context);

        if (user.isAdmin() && !request.isAdmin() && UserDao.countAdmins() <= 1) {
            Htmx.redirect(context, "/admin/users/" + user.getId() + "/edit?error=At%20least%20one%20admin%20user%20must%20remain");
            return;
        }

        user.setRank(request.rank());
        user.setCmsRole(request.cmsRole());
        UserDao.save(user);
        Htmx.redirect(context, "/admin/users/" + user.getId() + "/edit?notice=User%20updated");
    }

    private UserEntity requireUser(int id) {
        UserEntity user = UserDao.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user id " + id);
        }
        return user;
    }

    private Map<String, Object> userSummary(UserEntity user) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", user.getId());
        model.put("username", user.getUsername());
        model.put("email", user.getEmail());
        model.put("rank", user.getRank());
        model.put("cmsRole", user.getCmsRole());
        model.put("credits", user.getCredits());
        model.put("lastOnline", user.getLastOnline());
        return model;
    }

    private Map<String, Object> userEditor(UserEntity user) {
        Map<String, Object> model = userSummary(user);
        model.put("figure", user.getFigure());
        model.put("motto", user.getMotto());
        model.put("admin", user.isAdmin());
        return model;
    }
}
