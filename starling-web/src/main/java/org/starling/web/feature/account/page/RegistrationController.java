package org.starling.web.feature.account.page;

import io.javalin.http.Context;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.web.feature.shared.page.PublicPageModelFactory;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.request.RequestValues;
import org.starling.web.user.UserSessionService;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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

        String referral = resolveReferral(
                RequestValues.valueOrEmpty(context.queryParam("referral")),
                RequestValues.valueOrEmpty(context.sessionAttribute("registerReferral"))
        );
        UserEntity inviter = referral.isBlank() ? null : UserDao.findByUsername(referral);
        if (inviter != null) {
            context.sessionAttribute("registerReferral", inviter.getUsername());
        }

        boolean captchaInvalid = Boolean.TRUE.equals(context.sessionAttribute("registerCaptchaInvalid"));
        String errorCode = RequestValues.valueOrEmpty(context.queryParam("error"));

        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        model.put("referral", referral);
        model.put("registerReferral", inviter != null ? inviter.getUsername() : "");
        model.put("registerInviterName", inviter != null ? inviter.getUsername() : "");
        model.put("registerInviterFigure", inviter != null ? inviter.getFigure() : "");
        model.put("randomNum", System.currentTimeMillis() % 10000);
        model.put("randomFemaleFigure1", "hr-100-42.hd-180-1.ch-210-66.lg-270-82.sh-290-91");
        model.put("randomFemaleFigure2", "hr-100-61.hd-600-1.ch-255-62.lg-280-82.sh-300-64");
        model.put("randomFemaleFigure3", "hr-515-45.hd-600-2.ch-255-92.lg-720-82.sh-730-64");
        model.put("randomMaleFigure1", DEFAULT_FIGURE);
        model.put("randomMaleFigure2", "hr-165-42.hd-190-1.ch-255-66.lg-280-82.sh-305-64");
        model.put("randomMaleFigure3", "hr-828-61.hd-180-1.ch-210-66.lg-270-82.sh-290-91");
        model.put("registerCaptchaInvalid", captchaInvalid);
        model.put("registerUsername", RequestValues.valueOrEmpty(context.sessionAttribute("registerUsername")));
        model.put("registerShowPassword", RequestValues.valueOrEmpty(context.sessionAttribute("registerShowPassword")));
        model.put("registerFigure", RequestValues.valueOrDefault(context.sessionAttribute("registerFigure"), DEFAULT_FIGURE));
        model.put("registerGender", RequestValues.valueOrDefault(context.sessionAttribute("registerGender"), "M"));
        model.put("registerEmail", RequestValues.valueOrEmpty(context.sessionAttribute("registerEmail")));
        model.put("registerDay", RequestValues.valueOrEmpty(context.sessionAttribute("registerDay")));
        model.put("registerMonth", RequestValues.valueOrEmpty(context.sessionAttribute("registerMonth")));
        model.put("registerYear", RequestValues.valueOrEmpty(context.sessionAttribute("registerYear")));
        model.put("registerDayInt", RequestValues.parseInt(RequestValues.valueOrEmpty(context.sessionAttribute("registerDay")), 0));
        model.put("registerMonthInt", RequestValues.parseInt(RequestValues.valueOrEmpty(context.sessionAttribute("registerMonth")), 0));
        model.put("registerYearInt", RequestValues.parseInt(RequestValues.valueOrEmpty(context.sessionAttribute("registerYear")), 0));
        model.put("registerNameError", nameError(errorCode));
        model.put("registerPasswordError", passwordError(errorCode));
        model.put("registerBirthdayError", birthdayError(errorCode));
        model.put("registerEmailError", emailError(errorCode));
        model.put("registerTermsError", termsError(errorCode));
        model.put("registerFormError", formError(errorCode));
        model.put("registerYearOptions", yearOptions());
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

        if (!isValidUsername(request.username())) {
            context.redirect("/register?error=bad_username_invalid");
            return;
        }

        if (request.username().length() > 24) {
            context.redirect("/register?error=bad_username_length");
            return;
        }

        if (request.username().regionMatches(true, 0, "MOD-", 0, 4)) {
            context.redirect("/register?error=bad_username_reserved");
            return;
        }

        if (UserDao.findByUsername(request.username()) != null) {
            context.redirect("/register?error=bad_username_taken");
            return;
        }

        if (!request.password().equals(request.retypedPassword()) || request.password().length() < 6) {
            context.redirect("/register?error=bad_password");
            return;
        }

        if (!isValidBirthDate(request.day(), request.month(), request.year())) {
            context.redirect("/register?error=bad_birthday");
            return;
        }

        if (!request.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            context.redirect("/register?error=bad_email_invalid");
            return;
        }

        if (!request.email().equalsIgnoreCase(request.retypedEmail())) {
            context.redirect("/register?error=bad_email_mismatch");
            return;
        }

        if (UserDao.findByEmail(request.email()) != null) {
            context.redirect("/register?error=bad_email_taken");
            return;
        }

        if (!request.termsAccepted()) {
            context.redirect("/register?error=bad_terms");
            return;
        }

        String expectedCaptcha = RequestValues.valueOrEmpty(context.sessionAttribute("registerCaptchaText"));
        if (expectedCaptcha.isBlank() || !request.captchaResponse().equalsIgnoreCase(expectedCaptcha)) {
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
     * Checks whether a registration name is available using the PHPRetro ajax contract.
     * @param context the request context
     */
    public void nameCheck(Context context) {
        String name = RequestValues.valueOrEmpty(context.formParam("name")).trim();
        String error = null;

        if (UserDao.findByUsername(name) != null) {
            error = "This name is already in use.";
        } else if (!isValidUsername(name)) {
            error = "Your name can only contain letters, numbers and -=?!@:.";
        } else if (name.length() > 24 || name.isBlank()) {
            error = "Your name must be between 1 and 24 characters.";
        } else if (name.regionMatches(true, 0, "MOD-", 0, 4)) {
            error = "This name is not allowed.";
        }

        context.header("X-JSON", error == null
                ? "{}"
                : "{\"registration_name\":\"" + escapeJson(error) + "\"}");
        context.result("");
    }

    /**
     * Accepts the PHPRetro registration debug callback without returning a 404.
     * @param context the request context
     */
    public void registrationDebug(Context context) {
        context.status(204);
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
        context.sessionAttribute("registerReferral", request.referral());
        context.sessionAttribute("registerCaptchaInvalid", false);
    }

    private void clearRegisterState(Context context) {
        context.sessionAttribute("registerReferral", null);
        context.sessionAttribute("registerCaptchaInvalid", null);
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

    private String resolveReferral(String explicitReferral, String sessionReferral) {
        String normalized = RequestValues.valueOrEmpty(explicitReferral).trim();
        if (!normalized.isBlank()) {
            return normalized;
        }
        return RequestValues.valueOrEmpty(sessionReferral).trim();
    }

    private boolean isValidUsername(String username) {
        return username.matches("[A-Za-z0-9\\-=?!@:.]+");
    }

    private boolean isValidBirthDate(String dayRaw, String monthRaw, String yearRaw) {
        int day = RequestValues.parseInt(dayRaw, -1);
        int month = RequestValues.parseInt(monthRaw, -1);
        int year = RequestValues.parseInt(yearRaw, -1);
        return day >= 1 && day <= 31 && month >= 1 && month <= 12 && year >= 1920 && year <= 2008;
    }

    private List<Integer> yearOptions() {
        return IntStream.iterate(2008, year -> year >= 1900, year -> year - 1)
                .boxed()
                .toList();
    }

    private String nameError(String errorCode) {
        return switch (errorCode) {
            case "bad_username_taken" -> "This name is already in use.";
            case "bad_username_invalid" -> "Your name can only contain letters, numbers and -=?!@:.";
            case "bad_username_length" -> "Your name must be between 1 and 24 characters.";
            case "bad_username_reserved" -> "This name is not allowed.";
            default -> "";
        };
    }

    private String passwordError(String errorCode) {
        return "bad_password".equals(errorCode)
                ? "Your passwords do not match, or your password is too short."
                : "";
    }

    private String birthdayError(String errorCode) {
        return "bad_birthday".equals(errorCode)
                ? "Please enter a valid date of birth."
                : "";
    }

    private String emailError(String errorCode) {
        return switch (errorCode) {
            case "bad_email_invalid" -> "Please enter a valid email address.";
            case "bad_email_mismatch" -> "Emails don't match.";
            case "bad_email_taken" -> "The email entered is already used by someone else.";
            default -> "";
        };
    }

    private String termsError(String errorCode) {
        return "bad_terms".equals(errorCode)
                ? "Please read and accept the Terms of Use and Privacy Policy."
                : "";
    }

    private String formError(String errorCode) {
        return "blank_fields".equals(errorCode)
                ? "Please fill in all required fields before continuing."
                : "";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
