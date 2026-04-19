package org.starling.web.me;

public enum MailboxLabel {
    INBOX("inbox"),
    SENT("sent"),
    TRASH("trash");

    private final String key;

    MailboxLabel(String key) {
        this.key = key;
    }

    /**
     * Returns the route/query key used by the UI.
     * @return the mailbox key
     */
    public String key() {
        return key;
    }

    /**
     * Parses a mailbox label with an inbox fallback.
     * @param value the raw value
     * @return the parsed label
     */
    public static MailboxLabel from(String value) {
        if (value == null || value.isBlank()) {
            return INBOX;
        }

        for (MailboxLabel label : values()) {
            if (label.key.equalsIgnoreCase(value.trim())) {
                return label;
            }
        }

        return INBOX;
    }
}
