package org.oldskooler.vibe.game.messenger;

/**
 * Immutable messenger message payload.
 */
public final class MessengerMessage {

    private final int id;
    private final int toId;
    private final int fromId;
    private final long timeSet;
    private final String message;

    /**
     * Creates a new MessengerMessage.
     * @param id the id value
     * @param toId the recipient id value
     * @param fromId the sender id value
     * @param timeSet the send time value
     * @param message the message value
     */
    public MessengerMessage(int id, int toId, int fromId, long timeSet, String message) {
        this.id = id;
        this.toId = toId;
        this.fromId = fromId;
        this.timeSet = timeSet;
        this.message = message == null ? "" : message;
    }

    /**
     * Returns the id.
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the recipient id.
     * @return the recipient id
     */
    public int getToId() {
        return toId;
    }

    /**
     * Returns the sender id.
     * @return the sender id
     */
    public int getFromId() {
        return fromId;
    }

    /**
     * Returns the send time.
     * @return the send time
     */
    public long getTimeSet() {
        return timeSet;
    }

    /**
     * Returns the message body.
     * @return the message body
     */
    public String getMessage() {
        return message;
    }
}
