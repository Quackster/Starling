package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.me.MailboxLabel;
import org.starling.web.request.RequestValues;
import org.starling.web.service.ArticleService;
import org.starling.web.service.HotCampaignService;
import org.starling.web.service.MinimailService;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.CmsViewModelFactory;
import org.starling.web.view.PublicFeatureContentFactory;
import org.starling.web.view.PublicPageModelFactory;
import org.starling.web.view.UserViewModelFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MeController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final ArticleService articleService;
    private final HotCampaignService hotCampaignService;
    private final MinimailService minimailService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final PublicFeatureContentFactory publicFeatureContentFactory;
    private final UserViewModelFactory userViewModelFactory;
    private final CmsViewModelFactory cmsViewModelFactory;

    /**
     * Creates a new MeController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param articleService the article service
     * @param publicPageModelFactory the public page model factory
     * @param publicFeatureContentFactory the public feature content factory
     * @param userViewModelFactory the user view model factory
     * @param cmsViewModelFactory the CMS view model factory
     */
    public MeController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            ArticleService articleService,
            HotCampaignService hotCampaignService,
            MinimailService minimailService,
            PublicPageModelFactory publicPageModelFactory,
            PublicFeatureContentFactory publicFeatureContentFactory,
            UserViewModelFactory userViewModelFactory,
            CmsViewModelFactory cmsViewModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.articleService = articleService;
        this.hotCampaignService = hotCampaignService;
        this.minimailService = minimailService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.publicFeatureContentFactory = publicFeatureContentFactory;
        this.userViewModelFactory = userViewModelFactory;
        this.cmsViewModelFactory = cmsViewModelFactory;
    }

    /**
     * Renders the public user home.
     * @param context the request context
     */
    public void me(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "me", "me");
        List<Map<String, Object>> featuredArticles = articleService.listPublished().stream()
                .limit(5)
                .map(cmsViewModelFactory::articleSummary)
                .toList();

        for (int index = 0; index < 5; index++) {
            model.put("article" + (index + 1), index < featuredArticles.size()
                    ? featuredArticles.get(index)
                    : cmsViewModelFactory.emptyFeaturedArticle(index + 1));
        }

        model.put("currentUser", userViewModelFactory.create(currentUser.get()));
        model.put("onlineFriends", publicFeatureContentFactory.onlineFriends());
        model.put("hotCampaigns", hotCampaignService.listVisible());
        model.put("tagCloud", publicFeatureContentFactory.tagCloud());
        model.put("minimail", minimailService.buildView(
                currentUser.get(),
                mailboxLabel(context),
                unreadOnly(context),
                requestedMailPage(context),
                selectedMessageId(context),
                composeMode(context),
                replyMode(context),
                takeSessionValue(context, "minimailComposeRecipients"),
                takeSessionValue(context, "minimailComposeSubject"),
                takeSessionValue(context, "minimailComposeBody"),
                takeSessionValue(context, "minimailNotice"),
                takeSessionValue(context, "minimailError")
        ));
        context.html(templateRenderer.render("me", model));
    }

    /**
     * Sends a new minimail message.
     * @param context the request context
     */
    public void composeMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        String recipients = RequestValues.valueOrEmpty(context.formParam("recipients"));
        String subject = RequestValues.valueOrEmpty(context.formParam("subject"));
        String body = RequestValues.valueOrEmpty(context.formParam("body"));

        try {
            String notice = minimailService.sendMessage(currentUser.get(), recipients, subject, body);
            clearComposeState(context);
            context.sessionAttribute("minimailNotice", notice);
            context.redirect("/me?mailbox=inbox");
        } catch (IllegalArgumentException e) {
            context.sessionAttribute("minimailError", e.getMessage());
            context.sessionAttribute("minimailComposeRecipients", recipients);
            context.sessionAttribute("minimailComposeSubject", subject);
            context.sessionAttribute("minimailComposeBody", body);
            context.redirect("/me?mailbox=inbox&compose=true");
        }
    }

    /**
     * Sends a minimail reply.
     * @param context the request context
     */
    public void replyToMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int messageId = RequestValues.parseInt(context.pathParam("messageId"), 0);
        String body = RequestValues.valueOrEmpty(context.formParam("body"));

        try {
            String notice = minimailService.replyToMessage(currentUser.get(), messageId, body);
            clearComposeState(context);
            context.sessionAttribute("minimailNotice", notice);
            context.redirect("/me?mailbox=inbox&messageId=" + messageId);
        } catch (IllegalArgumentException e) {
            context.sessionAttribute("minimailError", e.getMessage());
            context.sessionAttribute("minimailComposeBody", body);
            context.redirect("/me?mailbox=inbox&messageId=" + messageId + "&reply=true");
        }
    }

    /**
     * Deletes a minimail row.
     * @param context the request context
     */
    public void deleteMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int messageId = RequestValues.parseInt(context.pathParam("messageId"), 0);
        MailboxLabel mailboxLabel = MailboxLabel.from(context.formParam("mailbox"));

        try {
            context.sessionAttribute("minimailNotice", minimailService.deleteMessage(currentUser.get(), messageId, mailboxLabel));
        } catch (IllegalArgumentException e) {
            context.sessionAttribute("minimailError", e.getMessage());
        }

        context.redirect(redirectToMailbox(mailboxLabel, context));
    }

    /**
     * Restores a trashed minimail row.
     * @param context the request context
     */
    public void restoreMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int messageId = RequestValues.parseInt(context.pathParam("messageId"), 0);
        context.sessionAttribute("minimailNotice", minimailService.restoreMessage(currentUser.get(), messageId));
        context.redirect("/me?mailbox=trash");
    }

    /**
     * Empties the current user's minimail trash.
     * @param context the request context
     */
    public void emptyTrash(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        context.sessionAttribute("minimailNotice", minimailService.emptyTrash(currentUser.get()));
        context.redirect("/me?mailbox=trash");
    }

    /**
     * Renders the post-registration welcome page.
     * @param context the request context
     */
    public void welcome(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "me", "me");
        model.put("currentUser", userViewModelFactory.create(currentUser.get()));
        model.put("welcomeRooms", List.of(
                Map.of("id", 0, "label", "Sunset Lounge"),
                Map.of("id", 1, "label", "Neon Loft"),
                Map.of("id", 2, "label", "Rooftop Club"),
                Map.of("id", 3, "label", "Cinema Suite"),
                Map.of("id", 4, "label", "Arcade Den"),
                Map.of("id", 5, "label", "Pool Deck")
        ));
        context.html(templateRenderer.render("welcome", model));
    }

    private Optional<UserEntity> currentUserOrRedirect(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
        }
        return currentUser;
    }

    private MailboxLabel mailboxLabel(Context context) {
        return MailboxLabel.from(context.queryParam("mailbox"));
    }

    private boolean unreadOnly(Context context) {
        return mailboxLabel(context) == MailboxLabel.INBOX
                && "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("unreadOnly")));
    }

    private int requestedMailPage(Context context) {
        return Math.max(1, RequestValues.parseInt(context.queryParam("mailPage"), 1));
    }

    private Integer selectedMessageId(Context context) {
        int messageId = RequestValues.parseInt(context.queryParam("messageId"), 0);
        return messageId > 0 ? messageId : null;
    }

    private boolean composeMode(Context context) {
        return "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("compose")));
    }

    private boolean replyMode(Context context) {
        return "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("reply")));
    }

    private String redirectToMailbox(MailboxLabel mailboxLabel, Context context) {
        StringBuilder target = new StringBuilder("/me?mailbox=").append(mailboxLabel.key());
        if (mailboxLabel == MailboxLabel.INBOX
                && "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.formParam("unreadOnly")))) {
            target.append("&unreadOnly=true");
        }
        int page = Math.max(1, RequestValues.parseInt(context.formParam("mailPage"), 1));
        if (page > 1) {
            target.append("&mailPage=").append(page);
        }
        return target.toString();
    }

    private String takeSessionValue(Context context, String key) {
        String value = RequestValues.valueOrEmpty(context.sessionAttribute(key));
        context.sessionAttribute(key, null);
        return value;
    }

    private void clearComposeState(Context context) {
        context.sessionAttribute("minimailComposeRecipients", null);
        context.sessionAttribute("minimailComposeSubject", null);
        context.sessionAttribute("minimailComposeBody", null);
        context.sessionAttribute("minimailError", null);
    }
}
