package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.CreditsPageContentFactory;

import java.util.Map;
import java.util.Optional;

public final class CreditsHabbletController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final CreditsPageContentFactory creditsPageContentFactory;

    /**
     * Creates a new CreditsHabbletController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param creditsPageContentFactory the credits page content factory
     */
    public CreditsHabbletController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            CreditsPageContentFactory creditsPageContentFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.creditsPageContentFactory = creditsPageContentFactory;
    }

    /**
     * Handles voucher redeem attempts.
     * @param context the request context
     */
    public void redeemVoucher(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        String message = currentUser.isPresent()
                ? "Voucher redemption is not enabled yet in Starling."
                : "Please sign in before redeeming a voucher.";

        Map<String, Object> model = Map.of("purse", creditsPageContentFactory.purse(currentUser, message));
        context.html(templateRenderer.render("habblet/credits_purse", model));
    }
}
