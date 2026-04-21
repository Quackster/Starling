package org.oldskooler.vibe.web.feature.me.referral;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.feature.me.friends.WebMessengerDao;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.request.RequestValues;
import org.oldskooler.vibe.web.site.SiteBranding;
import org.oldskooler.vibe.web.user.UserSessionService;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ReferralHabbletController {

    private static final int SEARCH_PAGE_SIZE = 5;
    private static final int FRIEND_LIMIT_NONCLUB = 100;
    private static final int FRIEND_LIMIT_CLUB = 600;
    private static final DateTimeFormatter LAST_VISIT_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final ReferralService referralService;
    private final SiteBranding siteBranding;

    /**
     * Creates a new ReferralHabbletController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param referralService the referral service
     * @param siteBranding the site branding
     */
    public ReferralHabbletController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            ReferralService referralService,
            SiteBranding siteBranding
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.referralService = referralService;
        this.siteBranding = siteBranding;
    }

    /**
     * Renders the invite-link fragment.
     * @param context the request context
     */
    public void inviteLink(Context context) {
        Optional<UserEntity> currentUser = authenticate(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("site", siteModel());
        model.put("inviteLink", referralService.inviteLink(currentUser.get(), siteBranding));
        context.html(templateRenderer.render("habblet/me_referral_link", model));
    }

    /**
     * Renders the Lisbon search results fragment for adding friends.
     * @param context the request context
     */
    public void searchContent(Context context) {
        Optional<UserEntity> currentUser = authenticate(context);
        if (currentUser.isEmpty()) {
            return;
        }

        String searchString = RequestValues.valueOrEmpty(context.formParam("searchString")).trim();
        int requestedPage = Math.max(1, RequestValues.parseInt(context.formParam("pageNumber"), 1));
        InviteSearchPage searchPage = paginate(searchResults(currentUser.get(), searchString), requestedPage);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("site", siteModel());
        model.put("searchResults", searchPage.searchResults());
        model.put("currentPage", searchPage.currentPage());
        model.put("totalPages", searchPage.totalPages());
        model.put("previousPageId", searchPage.previousPageId());
        model.put("nextPageId", searchPage.nextPageId());
        model.put("messenger", new InviteSearchMessengerView(currentUser.get().getId()));
        context.html(templateRenderer.render("habblet/invite_search_content", model));
    }

    /**
     * Renders the friend-request confirmation dialog.
     * @param context the request context
     */
    public void confirmAddFriend(Context context) {
        Optional<UserEntity> currentUser = authenticate(context);
        if (currentUser.isEmpty()) {
            return;
        }

        UserEntity targetUser = UserDao.findById(accountId(context));
        if (targetUser == null) {
            context.result("");
            return;
        }

        context.html(templateRenderer.render("habblet/invite_confirm_add_friend", Map.of(
                "username", targetUser.getUsername()
        )));
    }

    /**
     * Renders the legacy add-friend dialog body.
     * @param context the request context
     */
    public void addFriend(Context context) {
        Optional<UserEntity> currentUser = authenticate(context);
        if (currentUser.isEmpty()) {
            return;
        }

        context.html(templateRenderer.render("habblet/invite_add_friend", Map.of(
                "message", createFriendRequestResponse(currentUser.get(), accountId(context))
        )));
    }

    /**
     * Returns the legacy add-friend javascript callback.
     * @param context the request context
     */
    public void add(Context context) {
        Optional<UserEntity> currentUser = authenticate(context);
        if (currentUser.isEmpty()) {
            return;
        }

        String message = createFriendRequestResponse(currentUser.get(), accountId(context));
        context.contentType("application/x-javascript");
        context.result("Dialog.showInfoDialog(\"add-friend-messages\", \"" + escapeJavascript(message) + "\", \"OK\");");
    }

    private Optional<UserEntity> authenticate(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
        }
        return currentUser;
    }

    private Map<String, Object> siteModel() {
        return Map.of(
                "siteName", siteBranding.siteName(),
                "sitePath", siteBranding.sitePath()
        );
    }

    private int accountId(Context context) {
        String rawValue = RequestValues.valueOrDefault(context.formParam("accountId"), context.queryParam("accountId"));
        return RequestValues.parseInt(rawValue, 0);
    }

    private List<InviteSearchResultView> searchResults(UserEntity currentUser, String searchString) {
        List<InviteSearchResultView> results = new ArrayList<>();
        for (UserEntity user : WebMessengerDao.searchUsers(searchString)) {
            if (user.getId() == currentUser.getId()) {
                continue;
            }
            results.add(new InviteSearchResultView(
                    user.getId(),
                    user.getUsername(),
                    user.getFigure(),
                    formatLastOnline(user.getLastOnline())
            ));
        }
        return results;
    }

    private InviteSearchPage paginate(List<InviteSearchResultView> results, int requestedPage) {
        if (results.isEmpty()) {
            return new InviteSearchPage(List.of(), 1, 0, -1, -1);
        }

        int totalPages = (int) Math.ceil(results.size() / (double) SEARCH_PAGE_SIZE);
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        int fromIndex = (currentPage - 1) * SEARCH_PAGE_SIZE;
        int toIndex = Math.min(fromIndex + SEARCH_PAGE_SIZE, results.size());
        int previousPageId = currentPage > 1 ? currentPage - 1 : -1;
        int nextPageId = currentPage < totalPages ? currentPage + 1 : -1;

        return new InviteSearchPage(
                results.subList(fromIndex, toIndex),
                currentPage,
                totalPages,
                previousPageId,
                nextPageId
        );
    }

    private String createFriendRequestResponse(UserEntity currentUser, int accountId) {
        UserEntity targetUser = UserDao.findById(accountId);
        if (targetUser == null) {
            return "There was an error finding the user for the friend request.";
        }
        if (accountId == currentUser.getId()) {
            return "There was an error processing your request.";
        }
        if (friendsLimitReached(currentUser)) {
            return "Your friends list is full.";
        }
        if (WebMessengerDao.friendExists(currentUser.getId(), accountId)
                || WebMessengerDao.friendExists(accountId, currentUser.getId())) {
            return "This person is already your friend";
        }
        if (WebMessengerDao.requestExists(accountId, currentUser.getId())) {
            return "There is already a friend request for this user.";
        }
        if (friendsLimitReached(targetUser)) {
            return "This user's friend list is full.";
        }
        if (targetUser.getAllowFriendRequests() <= 0) {
            return "This user does not accept friend requests at the moment.";
        }

        WebMessengerDao.ensureRequest(accountId, currentUser.getId());
        return "Friend request has been sent successfully.";
    }

    private boolean friendsLimitReached(UserEntity user) {
        return WebMessengerDao.countFriends(user.getId()) >= friendsLimit(user);
    }

    private int friendsLimit(UserEntity user) {
        return user.hasClubSubscription() ? FRIEND_LIMIT_CLUB : FRIEND_LIMIT_NONCLUB;
    }

    private String formatLastOnline(Timestamp lastOnline) {
        if (lastOnline == null) {
            return "";
        }

        return lastOnline.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(LAST_VISIT_FORMAT)
                .toUpperCase(Locale.ROOT);
    }

    private String escapeJavascript(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private record InviteSearchPage(
            List<InviteSearchResultView> searchResults,
            int currentPage,
            int totalPages,
            int previousPageId,
            int nextPageId
    ) {}

    public static final class InviteSearchResultView {

        private final int id;
        private final String name;
        private final String figure;
        private final String formattedLastOnline;

        private InviteSearchResultView(int id, String name, String figure, String formattedLastOnline) {
            this.id = id;
            this.name = name;
            this.figure = figure;
            this.formattedLastOnline = formattedLastOnline;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getFigure() {
            return figure;
        }

        public String getFormattedLastOnline() {
            return formattedLastOnline;
        }
    }

    public static final class InviteSearchMessengerView {

        private final int userId;

        private InviteSearchMessengerView(int userId) {
            this.userId = userId;
        }

        public boolean hasFriend(int friendId) {
            return WebMessengerDao.friendExists(userId, friendId);
        }
    }
}
