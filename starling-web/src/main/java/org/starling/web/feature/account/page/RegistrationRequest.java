package org.starling.web.feature.account.page;

import io.javalin.http.Context;
import org.starling.web.request.RequestValues;

public record RegistrationRequest(
        String username,
        String password,
        String retypedPassword,
        String email,
        String retypedEmail,
        String day,
        String month,
        String year,
        String figure,
        String gender,
        String captchaResponse,
        boolean termsAccepted,
        String referral
) {

    private static final String DEFAULT_FIGURE = "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64";

    /**
     * Creates a RegistrationRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static RegistrationRequest from(Context context) {
        String gender = RequestValues.valueOrDefault(context.formParam("bean.gender"), "M");
        String figure = RequestValues.valueOrDefault(context.formParam("bean.figure"), DEFAULT_FIGURE);
        String randomFigure = RequestValues.valueOrEmpty(context.formParam("randomFigure")).trim();
        if (!randomFigure.isBlank() && randomFigure.contains("-")) {
            gender = randomFigure.substring(0, 1);
            figure = randomFigure.substring(2);
        }

        return new RegistrationRequest(
                RequestValues.valueOrEmpty(context.formParam("bean.avatarName")).trim(),
                RequestValues.valueOrEmpty(context.formParam("password")),
                RequestValues.valueOrEmpty(context.formParam("retypedPassword")),
                RequestValues.valueOrEmpty(context.formParam("bean.email")).trim(),
                RequestValues.valueOrEmpty(context.formParam("bean.retypedEmail")).trim(),
                RequestValues.valueOrEmpty(context.formParam("bean.day")).trim(),
                RequestValues.valueOrEmpty(context.formParam("bean.month")).trim(),
                RequestValues.valueOrEmpty(context.formParam("bean.year")).trim(),
                figure,
                gender,
                RequestValues.valueOrEmpty(context.formParam("bean.captchaResponse")).trim(),
                "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.formParam("bean.termsOfServiceSelection")).trim()),
                RequestValues.valueOrEmpty(context.formParam("referral")).trim()
        );
    }

    /**
     * Returns whether required fields are blank.
     * @return true when required fields are blank
     */
    public boolean hasBlankRequiredFields() {
        return username.isBlank()
                || password.isBlank()
                || retypedPassword.isBlank()
                || email.isBlank()
                || retypedEmail.isBlank()
                || day.isBlank()
                || month.isBlank()
                || year.isBlank();
    }

    /**
     * Returns the masked password display.
     * @return the masked password value
     */
    public String maskedPassword() {
        return password.replaceAll("(?s).", "*");
    }
}
