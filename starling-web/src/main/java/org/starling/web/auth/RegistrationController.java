package org.starling.web.auth;

import io.javalin.http.Context;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.request.RegistrationRequest;
import org.starling.web.request.RequestValues;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.PublicPageModelFactory;

import java.util.Map;

public final class RegistrationController {

    private static final String DEFAULT_FIGURE = "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64";

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final PublicPageModelFactory publicPageModelFactory;

    /**
     * Creates a new RegistrationController.
     * @param templateRenderer the template renderer
     * @param userSessionService the public user session service
     * @param publicPageModelFactory the public page model factory
     */
    public RegistrationController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            PublicPageModelFactory publicPageModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.publicPageModelFactory = publicPageModelFactory;
    }

    /**
     * Renders the Lisbon-style register page.
     * @param context the request context
     */
    public void registerPage(Context context) {
        if (userSessionService.authenticate(context).isPresent()) {
            context.redirect("/me");
            return;
        }

        int referral = RequestValues.parseInt(
                RequestValues.valueOrEmpty(context.queryParam("referral")),
                RequestValues.parseInt(context.sessionAttribute("registerReferral"), 0)
        );
        if (referral > 0) {
            context.sessionAttribute("registerReferral", String.valueOf(referral));
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        model.put("referral", referral);
        model.put("randomNum", System.currentTimeMillis() % 10000);
        model.put("randomFemaleFigure1", "hr-100-42.hd-180-1.ch-210-66.lg-270-82.sh-290-91");
        model.put("randomFemaleFigure2", "hr-100-61.hd-600-1.ch-255-62.lg-280-82.sh-300-64");
        model.put("randomFemaleFigure3", "hr-515-45.hd-600-2.ch-255-92.lg-720-82.sh-730-64");
        model.put("randomMaleFigure1", DEFAULT_FIGURE);
        model.put("randomMaleFigure2", "hr-165-42.hd-190-1.ch-255-66.lg-280-82.sh-305-64");
        model.put("randomMaleFigure3", "hr-828-61.hd-180-1.ch-210-66.lg-270-82.sh-290-91");
        model.put("registerCaptchaInvalid", Boolean.TRUE.equals(context.sessionAttribute("registerCaptchaInvalid")));
        model.put("registerEmailInvalid", Boolean.TRUE.equals(context.sessionAttribute("registerEmailInvalid")));
        model.put("registerUsername", RequestValues.valueOrEmpty(context.sessionAttribute("registerUsername")));
        model.put("registerShowPassword", RequestValues.valueOrEmpty(context.sessionAttribute("registerShowPassword")));
        model.put("registerFigure", RequestValues.valueOrDefault(context.sessionAttribute("registerFigure"), DEFAULT_FIGURE));
        model.put("registerGender", RequestValues.valueOrDefault(context.sessionAttribute("registerGender"), "M"));
        model.put("registerEmail", RequestValues.valueOrEmpty(context.sessionAttribute("registerEmail")));
        model.put("registerDay", RequestValues.valueOrEmpty(context.sessionAttribute("registerDay")));
        model.put("registerMonth", RequestValues.valueOrEmpty(context.sessionAttribute("registerMonth")));
        model.put("registerYear", RequestValues.valueOrEmpty(context.sessionAttribute("registerYear")));
        context.html(templateRenderer.render("register", model));
    }

    /**
     * Handles the public register flow.
     * @param context the request context
     */
    public void register(Context context) {
        if (userSessionService.authenticate(context).isPresent()) {
            context.redirect("/me");
            return;
        }

        RegistrationRequest request = RegistrationRequest.from(context);
        persistRegisterState(context, request);

        if (request.hasBlankRequiredFields()) {
            context.redirect("/register?error=blank_fields");
            return;
        }

        if (!request.password().equals(request.retypedPassword()) || request.password().length() < 6) {
            context.redirect("/register?error=bad_password");
            return;
        }

        if (!request.username().matches("[A-Za-z0-9\\-=?!@:.]{2,32}") || UserDao.findByUsername(request.username()) != null) {
            context.redirect("/register?error=bad_username");
            return;
        }

        if (!request.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || UserDao.findByEmail(request.email()) != null) {
            context.sessionAttribute("registerEmailInvalid", true);
            context.redirect("/register?error=bad_email");
            return;
        }

        String captchaResponse = RequestValues.valueOrEmpty(context.formParam("bean.captchaResponse")).trim();
        String expectedCaptcha = RequestValues.valueOrEmpty(context.sessionAttribute("registerCaptchaText"));
        if (expectedCaptcha.isBlank() || !captchaResponse.equalsIgnoreCase(expectedCaptcha)) {
            context.sessionAttribute("registerCaptchaInvalid", true);
            context.redirect("/register?error=bad_captcha");
            return;
        }

        UserDao.save(UserEntity.createRegisteredUser(
                request.username(),
                request.password(),
                request.figure(),
                request.gender(),
                request.email()
        ));
        UserEntity createdUser = UserDao.findByUsername(request.username());
        if (createdUser == null) {
            throw new IllegalStateException("Registered user could not be loaded after insert");
        }

        UserDao.updateLogin(createdUser);
        clearRegisterState(context);
        userSessionService.start(context, createdUser);
        context.redirect("/welcome");
    }

    /**
     * Cancels the register flow.
     * @param context the request context
     */
    public void cancel(Context context) {
        clearRegisterState(context);
        context.redirect("/");
    }

    private void persistRegisterState(Context context, RegistrationRequest request) {
        context.sessionAttribute("registerUsername", request.username());
        context.sessionAttribute("registerShowPassword", request.maskedPassword());
        context.sessionAttribute("registerFigure", request.figure());
        context.sessionAttribute("registerGender", request.gender());
        context.sessionAttribute("registerEmail", request.email());
        context.sessionAttribute("registerDay", request.day());
        context.sessionAttribute("registerMonth", request.month());
        context.sessionAttribute("registerYear", request.year());
        context.sessionAttribute("registerCaptchaInvalid", false);
        context.sessionAttribute("registerEmailInvalid", false);
    }

    private void clearRegisterState(Context context) {
        context.sessionAttribute("registerReferral", null);
        context.sessionAttribute("registerCaptchaInvalid", null);
        context.sessionAttribute("registerEmailInvalid", null);
        context.sessionAttribute("registerCaptchaText", null);
        context.sessionAttribute("registerUsername", null);
        context.sessionAttribute("registerShowPassword", null);
        context.sessionAttribute("registerFigure", null);
        context.sessionAttribute("registerGender", null);
        context.sessionAttribute("registerEmail", null);
        context.sessionAttribute("registerDay", null);
        context.sessionAttribute("registerMonth", null);
        context.sessionAttribute("registerYear", null);
    }
}
