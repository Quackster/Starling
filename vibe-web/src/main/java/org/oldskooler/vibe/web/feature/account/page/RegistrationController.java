package org.oldskooler.vibe.web.feature.account.page;

import io.javalin.http.Context;
import org.oldskooler.vibe.json.GsonSupport;
import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.admin.auth.AdminSessionService;
import org.oldskooler.vibe.web.feature.me.referral.ReferralService;
import org.oldskooler.vibe.web.feature.shared.page.PublicPageModelFactory;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.request.RequestValues;
import org.oldskooler.vibe.web.user.UserSessionService;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class RegistrationController {

    private static final String DEFAULT_FIGURE = "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64";
    private static final String EMAIL_PATTERN = "(?i)^[a-z0-9_\\.-]+@([a-z0-9]+([\\-]+[a-z0-9]+)*\\.)+[a-z]{2,7}$";

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final AdminSessionService adminSessionService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final ReferralService referralService;

    /**
     * Creates a new RegistrationController.
     * @param templateRenderer the template renderer
     * @param userSessionService the public user session service
     * @param publicPageModelFactory the public page model factory
     * @param referralService the referral service
     */
    public RegistrationController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            AdminSessionService adminSessionService,
            PublicPageModelFactory publicPageModelFactory,
            ReferralService referralService
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.adminSessionService = adminSessionService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.referralService = referralService;
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

        renderRegisterPage(
                context,
                RegistrationViewState.fromSession(context),
                errorsFromLegacyCode(
                        RequestValues.valueOrEmpty(context.queryParam("error")),
                        Boolean.TRUE.equals(context.sessionAttribute("registerCaptchaInvalid"))
                )
        );
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

        RegistrationErrors errors = validateRequest(context, request);
        context.sessionAttribute("registerCaptchaInvalid", errors.captchaInvalid());
        if (errors.hasAny()) {
            renderRegisterPage(context, RegistrationViewState.fromRequest(request), errors);
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

        referralService.applyReferral(createdUser, request.referral());
        UserDao.updateLogin(createdUser);
        clearRegisterState(context);
        adminSessionService.revokeAccess(context);
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
            error = "Sorry, but this username is taken. Please choose another one.";
        } else if (name.isBlank()) {
            error = "Please enter a username.";
        } else if (!isValidUsername(name)) {
            error = "Sorry, but this username contains invalid characters.";
        } else if (name.length() > 24) {
            error = "Sorry, but this username is too long.";
        } else if (name.regionMatches(true, 0, "MOD-", 0, 4)) {
            error = "This name is not allowed.";
        }

        context.header("X-JSON", error == null
                ? "{}"
                : GsonSupport.toJson(Map.of("registration_name", error)));
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

    private void renderRegisterPage(Context context, RegistrationViewState viewState, RegistrationErrors errors) {
        String referral = resolveReferral(
                RequestValues.valueOrEmpty(context.queryParam("referral")),
                viewState.referral()
        );
        UserEntity inviter = referralService.findInviterByReferral(referral);
        if (inviter != null) {
            context.sessionAttribute("registerReferral", inviter.getUsername());
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "community");
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
        model.put("registerCaptchaInvalid", errors.captchaInvalid());
        model.put("registerUsername", viewState.username());
        model.put("registerFigure", viewState.figure());
        model.put("registerGender", viewState.gender());
        model.put("registerEmail", viewState.email());
        model.put("registerDay", viewState.day());
        model.put("registerMonth", viewState.month());
        model.put("registerYear", viewState.year());
        model.put("registerDayInt", RequestValues.parseInt(viewState.day(), 0));
        model.put("registerMonthInt", RequestValues.parseInt(viewState.month(), 0));
        model.put("registerYearInt", RequestValues.parseInt(viewState.year(), 0));
        model.put("registerNameError", errors.name());
        model.put("registerPasswordError", errors.password());
        model.put("registerBirthdayError", errors.birthday());
        model.put("registerEmailError", errors.email());
        model.put("registerTermsError", errors.terms());
        model.put("registerYearOptions", yearOptions());
        context.html(templateRenderer.render("register", model));
    }

    private RegistrationErrors validateRequest(Context context, RegistrationRequest request) {
        String nameError = "";
        String passwordError = "";
        String birthdayError = "";
        String emailError = "";
        String termsError = "";

        if (UserDao.findByUsername(request.username()) != null) {
            nameError = "This username is in use. Please choose another name.";
        } else if (!request.username().equals(request.username().replaceAll("[^A-Za-z0-9\\-=?!@:.]", ""))) {
            nameError = "Your username is invalid or contains invalid characters.";
        } else if (request.username().length() > 24) {
            nameError = "The name you have chosen is too long.";
        } else if (request.username().isBlank()) {
            nameError = "Please enter a username.";
        }

        if (request.username().regionMatches(true, 0, "MOD-", 0, 4)) {
            nameError = "This name is not allowed.";
        }

        if (!request.password().equals(request.retypedPassword())) {
            passwordError = "The passwords do not match. Please try again.";
        } else if (request.password().length() < 6) {
            passwordError = "Your password is too short.";
        }

        if (!isValidBirthDate(request.day(), request.month(), request.year())) {
            birthdayError = "Please supply a valid date of birth.";
        }

        if (request.email().length() < 6 || !request.email().matches(EMAIL_PATTERN)) {
            emailError = "Please supply a valid e-mail address.";
        } else if (!request.email().equals(request.retypedEmail())) {
            emailError = "The e-mail addresses don't match.";
        }

        if (!request.termsAccepted()) {
            termsError = "Please accept the terms of service";
        }

        String expectedCaptcha = RequestValues.valueOrEmpty(context.sessionAttribute("registerCaptchaText"));
        boolean captchaInvalid = expectedCaptcha.isBlank() || !request.captchaResponse().equalsIgnoreCase(expectedCaptcha);
        return new RegistrationErrors(nameError, passwordError, birthdayError, emailError, termsError, captchaInvalid);
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

    private RegistrationErrors errorsFromLegacyCode(String errorCode, boolean captchaInvalid) {
        return switch (errorCode) {
            case "bad_username_taken" -> new RegistrationErrors(
                    "This username is in use. Please choose another name.", "", "", "", "", captchaInvalid
            );
            case "bad_username_invalid" -> new RegistrationErrors(
                    "Your username is invalid or contains invalid characters.", "", "", "", "", captchaInvalid
            );
            case "bad_username_length" -> new RegistrationErrors(
                    "The name you have chosen is too long.", "", "", "", "", captchaInvalid
            );
            case "bad_username_reserved" -> new RegistrationErrors(
                    "This name is not allowed.", "", "", "", "", captchaInvalid
            );
            case "bad_password", "bad_password_mismatch" -> new RegistrationErrors(
                    "", "The passwords do not match. Please try again.", "", "", "", captchaInvalid
            );
            case "bad_password_short" -> new RegistrationErrors(
                    "", "Your password is too short.", "", "", "", captchaInvalid
            );
            case "bad_birthday" -> new RegistrationErrors(
                    "", "", "Please supply a valid date of birth.", "", "", captchaInvalid
            );
            case "bad_email_invalid" -> new RegistrationErrors(
                    "", "", "", "Please supply a valid e-mail address.", "", captchaInvalid
            );
            case "bad_email_mismatch" -> new RegistrationErrors(
                    "", "", "", "The e-mail addresses don't match.", "", captchaInvalid
            );
            case "bad_terms" -> new RegistrationErrors(
                    "", "", "", "", "Please accept the terms of service", captchaInvalid
            );
            default -> RegistrationErrors.none(captchaInvalid);
        };
    }

    private record RegistrationViewState(
            String username,
            String figure,
            String gender,
            String email,
            String day,
            String month,
            String year,
            String referral
    ) {
        private static RegistrationViewState fromSession(Context context) {
            return new RegistrationViewState(
                    RequestValues.valueOrEmpty(context.sessionAttribute("registerUsername")),
                    RequestValues.valueOrDefault(context.sessionAttribute("registerFigure"), DEFAULT_FIGURE),
                    RequestValues.valueOrDefault(context.sessionAttribute("registerGender"), "M"),
                    RequestValues.valueOrEmpty(context.sessionAttribute("registerEmail")),
                    RequestValues.valueOrEmpty(context.sessionAttribute("registerDay")),
                    RequestValues.valueOrEmpty(context.sessionAttribute("registerMonth")),
                    RequestValues.valueOrEmpty(context.sessionAttribute("registerYear")),
                    RequestValues.valueOrEmpty(context.sessionAttribute("registerReferral"))
            );
        }

        private static RegistrationViewState fromRequest(RegistrationRequest request) {
            return new RegistrationViewState(
                    request.username(),
                    request.figure(),
                    request.gender(),
                    request.email(),
                    request.day(),
                    request.month(),
                    request.year(),
                    request.referral()
            );
        }
    }

    private record RegistrationErrors(
            String name,
            String password,
            String birthday,
            String email,
            String terms,
            boolean captchaInvalid
    ) {
        private static RegistrationErrors none(boolean captchaInvalid) {
            return new RegistrationErrors("", "", "", "", "", captchaInvalid);
        }

        private boolean hasAny() {
            return captchaInvalid
                    || !name.isBlank()
                    || !password.isBlank()
                    || !birthday.isBlank()
                    || !email.isBlank()
                    || !terms.isBlank();
        }
    }
}
