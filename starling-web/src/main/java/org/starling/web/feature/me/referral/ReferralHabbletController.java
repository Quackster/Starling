package org.starling.web.feature.me.referral;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.site.SiteBranding;
import org.starling.web.user.UserSessionService;

import java.util.Map;
import java.util.Optional;

public final class ReferralHabbletController {

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
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.status(401).html("");
            return;
        }

        Map<String, Object> model = Map.of(
                "inviteLink", referralService.inviteLink(currentUser.get(), siteBranding),
                "referralReward", referralService.rewardCredits(),
                "referralCount", referralService.referralCount(currentUser.get())
        );
        context.html(templateRenderer.render("habblet/me_referral_link", model));
    }
}
