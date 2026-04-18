package org.starling.web.view;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.user.UserSessionService;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PublicPageModelFactory {

    private final UserSessionService userSessionService;
    private final UserViewModelFactory userViewModelFactory;

    /**
     * Creates a new PublicPageModelFactory.
     * @param userSessionService the public user session service
     * @param userViewModelFactory the user view model factory
     */
    public PublicPageModelFactory(UserSessionService userSessionService, UserViewModelFactory userViewModelFactory) {
        this.userSessionService = userSessionService;
        this.userViewModelFactory = userViewModelFactory;
    }

    /**
     * Builds the common public page model.
     * @param context the request context
     * @param currentPage the current page key
     * @return the resulting model
     */
    public Map<String, Object> create(Context context, String currentPage) {
        Map<String, Object> model = new HashMap<>();
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);

        Map<String, Object> site = new HashMap<>();
        site.put("siteName", "Starling");
        site.put("sitePath", "");
        site.put("staticContentPath", "");
        site.put("formattedUsersOnline", "0");
        site.put("visits", 0);
        site.put("serverOnline", true);
        site.put("playerName", currentUser.map(UserEntity::getUsername).orElse(""));

        Map<String, Object> session = new HashMap<>();
        session.put("loggedIn", currentUser.isPresent());
        session.put("currentPage", currentPage);

        String publicAlert = valueOrEmpty(context.sessionAttribute("publicAlert"));
        boolean hasAlert = !publicAlert.isBlank();
        Map<String, Object> alert = new HashMap<>();
        alert.put("hasAlert", hasAlert);
        alert.put("message", hasAlert ? publicAlert : "");
        alert.put("colour", "red");
        context.sessionAttribute("publicAlert", null);

        model.put("site", site);
        model.put("session", session);
        model.put("alert", alert);
        currentUser.ifPresent(user -> model.put("playerDetails", userViewModelFactory.create(user)));
        model.put("siteTitle", "Starling");
        model.put("year", Year.now().getValue());
        return model;
    }

    /**
     * Builds the public 404 page model.
     * @return the resulting model
     */
    public Map<String, Object> notFound() {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", "Starling");
        model.put("site", Map.of("siteName", "Starling", "sitePath", "", "staticContentPath", ""));
        model.put("session", Map.of("loggedIn", false, "currentPage", "community"));
        model.put("message", "That page could not be found.");
        return model;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
