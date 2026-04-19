package org.starling.web.feature.account.page;

import io.javalin.http.Context;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.web.feature.shared.page.PublicPageModelFactory;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.request.RequestValues;
import org.starling.web.user.UserSessionService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class AccountController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final PublicPageModelFactory publicPageModelFactory;

    /**
     * Creates a new AccountController.
     * @param templateRenderer the template renderer
     * @param userSessionService the public user session service
     * @param publicPageModelFactory the public page model factory
     */
    public AccountController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            PublicPageModelFactory publicPageModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.publicPageModelFactory = publicPageModelFactory;
    }

    /**
     * Renders the public account login popup.
     * @param context the request context
     */
    public void loginPage(Context context) {
        if (userSessionService.authenticate(context).isPresent()) {
            context.redirect("/me");
            return;
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        model.put("rememberMe", "true".equalsIgnoreCase(context.queryParam("rememberme")));
        model.put("username", RequestValues.valueOrEmpty(context.queryParam("username")));
        context.html(templateRenderer.render("account/login", model));
    }

    /**
     * Renders the Lisbon security check redirect.
     * @param context the request context
     */
    public void securityCheck(Context context) {
        String nextPath = RequestValues.valueOrDefault(context.sessionAttribute("postLoginPath"), "/me");
        context.sessionAttribute("postLoginPath", null);
        context.redirect(nextPath);
    }

    /**
     * Logs the public user out.
     * @param context the request context
     */
    public void logout(Context context) {
        renderLogout(context, RequestValues.valueOrEmpty(context.queryParam("reason")));
    }

    /**
     * Renders the PHPRetro-compatible logged out confirmation page.
     * @param context the request context
     */
    public void logoutOk(Context context) {
        renderLogout(context, "");
    }

    /**
     * Routes the client entry path.
     * @param context the request context
     */
    public void clientEntry(Context context) {
        context.redirect(userSessionService.authenticate(context).isPresent() ? "/me" : "/");
    }

    /**
     * Handles the Lisbon-style public account submit request.
     * @param context the request context
     */
    public void submit(Context context) {
        PublicLoginRequest request = PublicLoginRequest.from(context);

        UserEntity user = UserDao.findByUsernameOrEmail(request.username());
        if (user != null && user.getPassword().equals(request.password())) {
            UserDao.updateLogin(user);
            userSessionService.start(context, user, request.rememberMe());
            context.sessionAttribute("postLoginPath", "/me");
            context.redirect("/security_check");
            return;
        }

        context.sessionAttribute("publicAlert", "The username or password you entered is incorrect.");
        String encodedPage = URLEncoder.encode(request.page(), StandardCharsets.UTF_8);
        String encodedUsername = URLEncoder.encode(request.username(), StandardCharsets.UTF_8);
        context.redirect("/?page=" + encodedPage + "&username=" + encodedUsername + "&rememberme=" + request.rememberMeFlag());
    }

    private void renderLogout(Context context, String reasonCode) {
        userSessionService.clear(context);

        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        @SuppressWarnings("unchecked")
        Map<String, Object> site = (Map<String, Object>) model.get("site");
        String siteName = site == null ? "Habbo" : RequestValues.valueOrDefault((String) site.get("siteName"), "Habbo");

        String normalizedReason = RequestValues.valueOrEmpty(reasonCode).trim();
        boolean error = !normalizedReason.isBlank();
        String message = switch (normalizedReason) {
            case "" -> "You have successfully signed out";
            case "banned" -> "You have been banned for breaking the " + siteName + " way.";
            case "concurrentlogin" -> "You were automatically signed out because you signed in from another web browser or machine.";
            default -> null;
        };
        if (message == null) {
            context.redirect("/account/logout_ok");
            return;
        }

        model.put("logoutMessage", message);
        model.put("logoutError", error);
        context.html(templateRenderer.render("account/logout", model));
    }
}
