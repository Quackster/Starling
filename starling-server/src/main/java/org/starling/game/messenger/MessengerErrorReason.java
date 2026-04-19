package org.starling.game.messenger;

/**
 * Optional sub-reasons used by the console error packet.
 */
public enum MessengerErrorReason {
    FRIENDLIST_FULL_PENDING_FRIEND(1),
    SENDER_FRIENDLIST_FULL(2),
    CONCURRENCY(42);

    private final int reasonCode;

    MessengerErrorReason(int reasonCode) {
        this.reasonCode = reasonCode;
    }

    /**
     * Returns the reason code.
     * @return the reason code
     */
    public int getReasonCode() {
        return reasonCode;
    }
}
