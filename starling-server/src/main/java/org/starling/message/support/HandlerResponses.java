package org.starling.message.support;

import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;

public final class HandlerResponses {

    private static final String RAW_ZERO = "0";

    /**
     * Creates a new HandlerResponses.
     */
    private HandlerResponses() {}

    /**
     * Sends error.
     * @param session the session value
     * @param message the message value
     */
    public static void sendError(Session session, String message) {
        session.send(errorMessage(message));
    }

    /**
     * Errors message.
     * @param message the message value
     * @return the result of this operation
     */
    public static ServerMessage errorMessage(String message) {
        return new ServerMessage(OutgoingPackets.ERROR)
                .writeRaw(message == null ? "" : message);
    }

    /**
     * Sends success.
     * @param session the session value
     * @param originatingOpcode the originating opcode value
     */
    public static void sendSuccess(Session session, int originatingOpcode) {
        session.send(new ServerMessage(OutgoingPackets.SUCCESS).writeInt(originatingOpcode));
    }

    /**
     * Sends failure.
     * @param session the session value
     * @param originatingOpcode the originating opcode value
     * @param message the message value
     */
    public static void sendFailure(Session session, int originatingOpcode, String message) {
        session.send(new ServerMessage(OutgoingPackets.FAILURE)
                .writeInt(originatingOpcode)
                .writeString(message == null ? "" : message));
    }

    /**
     * Singles zero message.
     * @param opcode the opcode value
     * @return the result of this operation
     */
    public static ServerMessage singleZeroMessage(int opcode) {
        return new ServerMessage(opcode).writeRaw(RAW_ZERO);
    }
}
