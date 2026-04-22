package org.oldskooler.vibe.web.feature.account.page;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.admin.auth.AdminSessionService;
import org.oldskooler.vibe.web.feature.shared.page.PublicPageModelFactory;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.request.RequestValues;
import org.oldskooler.vibe.web.settings.WebSettingsService;
import org.oldskooler.vibe.web.user.UserSessionService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AccountController {

    private static final String POST_LOGIN_PATH_SESSION_KEY = "postLoginPath";
    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final AdminSessionService adminSessionService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final WebSettingsService webSettingsService;

    /**
     * Creates a new AccountController.
     * @param templateRenderer the template renderer
     * @param userSessionService the public user session service
     * @param publicPageModelFactory the public page model factory
     */
    public AccountController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            AdminSessionService adminSessionService,
            PublicPageModelFactory publicPageModelFactory,
            WebSettingsService webSettingsService
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.adminSessionService = adminSessionService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.webSettingsService = webSettingsService;
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
        model.put("page", RequestValues.valueOrEmpty(context.queryParam("page")));
        context.html(templateRenderer.render("account/login", model));
    }

    /**
     * Renders the Lisbon security check redirect.
     * @param context the request context
     */
    public void securityCheck(Context context) {
        if (userSessionService.authenticate(context).isEmpty()) {
            context.redirect("/");
            return;
        }

        String nextPath = resolvePostLoginPath(
                RequestValues.valueOrEmpty(context.queryParam("page")),
                RequestValues.valueOrEmpty(context.sessionAttribute(POST_LOGIN_PATH_SESSION_KEY)),
                "/"
        );
        context.sessionAttribute(POST_LOGIN_PATH_SESSION_KEY, null);
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
     * Renders the legacy logged out confirmation page.
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
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
            return;
        }
        if ("reauthenticate".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("x")))) {
            context.sessionAttribute(UserSessionService.REAUTHENTICATE_PATH_SESSION_KEY, "/client");
            context.redirect("/account/reauthenticate");
            return;
        }
        if (userSessionService.isReauthenticationRequired(context)) {
            context.sessionAttribute(UserSessionService.REAUTHENTICATE_PATH_SESSION_KEY, currentPath(context));
            context.redirect("/account/reauthenticate");
            return;
        }

        UserDao.markOnline(currentUser.get().getId());
        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        model.put("user", Map.of(
                "username", currentUser.get().getUsername(),
                "ssoTicket", RequestValues.valueOrDefault(currentUser.get().getSsoTicket(), "")
        ));
        model.put("wide", !"false".equalsIgnoreCase(context.queryParam("wide")));
        model.put("onlineCount", UserDao.countOnline());
        model.put("client", clientSettings());
        context.html(templateRenderer.render("account/client", model));
    }

    /**
     * Renders the reauthentication page.
     * @param context the request context
     */
    public void reauthenticatePage(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
            return;
        }

        Map<String, Object> model = reauthenticationModel(context, currentUser.orElseThrow(), RequestValues.valueOrEmpty(context.queryParam("error")));
        context.html(templateRenderer.render("account/reauthenticate", model));
    }

    /**
     * Confirms the current password and restores the protected session.
     * @param context the request context
     */
    public void reauthenticate(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
            return;
        }

        String password = RequestValues.valueOrEmpty(context.formParam("password"));
        if (!currentUser.get().getPassword().equals(password)) {
            Map<String, Object> model = reauthenticationModel(context, currentUser.get(), "Invalid password.");
            context.status(401).html(templateRenderer.render("account/reauthenticate", model));
            return;
        }

        userSessionService.restartAfterReauthentication(context, currentUser.get());
        String nextPath = resolvePostLoginPath(
                RequestValues.valueOrEmpty(context.formParam("page")),
                RequestValues.valueOrDefault(context.sessionAttribute(UserSessionService.REAUTHENTICATE_PATH_SESSION_KEY), "/client"),
                "/client"
        );
        context.sessionAttribute(UserSessionService.REAUTHENTICATE_PATH_SESSION_KEY, null);
        context.sessionAttribute(POST_LOGIN_PATH_SESSION_KEY, nextPath);
        context.redirect(securityCheckLocation(nextPath));
    }

    /**
     * Returns a client cache-check result.
     * @param context the request context
     */
    public void cacheCheck(Context context) {
        context.result("true");
    }

    /**
     * Returns the live online count payload expected by habboclient.js.
     * @param context the request context
     */
    public void updateHabboCount(Context context) {
        context.json(Map.of("habboCountText", UserDao.countOnline() + " members online"));
    }

    /**
     * Marks the current client session offline.
     * @param context the request context
     */
    public void unloadClient(Context context) {
        userSessionService.authenticate(context).ifPresent(user -> UserDao.markOffline(user.getId()));
        context.status(204);
    }

    /**
     * Accepts legacy client log callbacks.
     * @param context the request context
     */
    public void clientLog(Context context) {
        context.status(204);
    }

    /**
     * Renders lightweight client error pages expected by the Shockwave shell.
     * @param context the request context
     */
    public void clientUtils(Context context) {
        String key = RequestValues.valueOrDefault(context.queryParam("key"), "connection_failed");
        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        String siteName = webSettingsService.siteName();
        String normalizedKey = switch (key) {
            case "install_shockwave", "upgrade_shockwave", "error" -> key;
            default -> "connection_failed";
        };
        Map<String, Object> clientUtils = new LinkedHashMap<>();
        clientUtils.put("key", normalizedKey);
        clientUtils.put("pageName", "Error");
        clientUtils.put("habboName", userSessionService.authenticate(context).map(UserEntity::getUsername).orElse(""));
        clientUtils.put("errorId", RequestValues.valueOrEmpty(context.queryParam("error_id")));
        clientUtils.put("connectionTitle", "Connection to " + siteName + " failed.");
        clientUtils.put(
                "connectionMessage",
                "Unfortunately we are unable to connect you to " + siteName
                        + ". This could be because your computer is blocking the connections via a firewall. "
                        + "Please verify with the person responsible for your Internet connection that the following addresses are permitted by the firewall:"
        );
        clientUtils.put("hotelIp", webSettingsService.clientHotelIp());
        clientUtils.put("hotelPort", webSettingsService.clientHotelPort());
        model.put("clientUtils", clientUtils);
        context.html(templateRenderer.render("account/client_error", model));
    }

    /**
     * Renders the Shockwave install helper page.
     * @param context the request context
     */
    public void installShockwave(Context context) {
        context.redirect("/clientutils?key=install_shockwave");
    }

    /**
     * Renders the Shockwave upgrade helper page.
     * @param context the request context
     */
    public void upgradeShockwave(Context context) {
        context.redirect("/clientutils?key=upgrade_shockwave");
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
            if (webSettingsService.resetSsoTicketOnLogin()) {
                user = UserDao.rotateSsoTicket(user);
            }
            adminSessionService.revokeAccess(context);
            userSessionService.start(context, user, request.rememberMe());
            String nextPath = resolvePostLoginPath(
                    request.page(),
                    RequestValues.valueOrEmpty(context.sessionAttribute(POST_LOGIN_PATH_SESSION_KEY)),
                    "/"
            );
            context.sessionAttribute(POST_LOGIN_PATH_SESSION_KEY, nextPath);
            context.redirect(securityCheckLocation(nextPath));
            return;
        }

        context.sessionAttribute("publicAlert", "The username or password you entered is incorrect.");
        String encodedPage = URLEncoder.encode(request.page(), StandardCharsets.UTF_8);
        String encodedUsername = URLEncoder.encode(request.username(), StandardCharsets.UTF_8);
        context.redirect("/?page=" + encodedPage + "&username=" + encodedUsername + "&rememberme=" + request.rememberMeFlag());
    }

    private void renderLogout(Context context, String reasonCode) {
        adminSessionService.revokeAccess(context);
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

    private Map<String, Object> clientSettings() {
        return Map.of(
                "dcr", webSettingsService.clientDcr(),
                "externalVariables", webSettingsService.clientExternalVariables(),
                "externalTexts", webSettingsService.clientExternalTexts(),
                "loaderTimeoutMs", webSettingsService.clientLoaderTimeoutMs(),
                "hotelIp", webSettingsService.clientHotelIp(),
                "hotelPort", webSettingsService.clientHotelPort(),
                "hotelMusPort", webSettingsService.clientHotelMusPort()
        );
    }

    private Map<String, Object> reauthenticationModel(Context context, UserEntity currentUser, String error) {
        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        model.put("error", error);
        model.put("reauthUser", currentUser);
        model.put("reauthPath", RequestValues.valueOrDefault(context.sessionAttribute(UserSessionService.REAUTHENTICATE_PATH_SESSION_KEY), "/client"));
        return model;
    }

    private String currentPath(Context context) {
        String queryString = context.queryString();
        return queryString == null || queryString.isBlank()
                ? context.path()
                : context.path() + "?" + queryString;
    }

    private String securityCheckLocation(String nextPath) {
        return "/security_check?page=" + URLEncoder.encode(nextPath, StandardCharsets.UTF_8);
    }

    private String resolvePostLoginPath(String requestedPath, String storedPath, String defaultPath) {
        String normalizedRequestedPath = normalizePostLoginPath(requestedPath);
        if (!normalizedRequestedPath.isBlank()) {
            return normalizedRequestedPath;
        }

        String normalizedStoredPath = normalizePostLoginPath(storedPath);
        if (!normalizedStoredPath.isBlank()) {
            return normalizedStoredPath;
        }

        String normalizedDefaultPath = normalizePostLoginPath(defaultPath);
        return normalizedDefaultPath.isBlank() ? "/" : normalizedDefaultPath;
    }

    private String normalizePostLoginPath(String value) {
        String normalized = RequestValues.valueOrEmpty(value).trim();
        if (normalized.isBlank()) {
            return "";
        }

        if (normalized.startsWith("./")) {
            normalized = normalized.substring(1);
        }

        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("//")) {
            return "";
        }

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return normalized.startsWith("/security_check") ? "" : normalized;
    }
}
