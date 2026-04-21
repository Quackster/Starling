package org.oldskooler.vibe.game.messenger;

/**
 * Structured messenger error.
 */
public final class MessengerError {

    private String causer = "";
    private final MessengerErrorType errorType;
    private final MessengerErrorReason errorReason;

    /**
     * Creates a new MessengerError.
     * @param errorType the error type value
     */
    public MessengerError(MessengerErrorType errorType) {
        this(errorType, null);
    }

    /**
     * Creates a new MessengerError.
     * @param errorType the error type value
     * @param errorReason the error reason value
     */
    public MessengerError(MessengerErrorType errorType, MessengerErrorReason errorReason) {
        this.errorType = errorType;
        this.errorReason = errorReason;
    }

    /**
     * Returns the causer.
     * @return the causer
     */
    public String getCauser() {
        return causer;
    }

    /**
     * Sets the causer.
     * @param causer the causer value
     */
    public void setCauser(String causer) {
        this.causer = causer == null ? "" : causer;
    }

    /**
     * Returns the error type.
     * @return the error type
     */
    public MessengerErrorType getErrorType() {
        return errorType;
    }

    /**
     * Returns the error reason.
     * @return the error reason
     */
    public MessengerErrorReason getErrorReason() {
        return errorReason;
    }
}
