package org.starling.message.support;

import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;

public final class HandlerResponses {

    private static final String RAW_ZERO = "0";

    private HandlerResponses() {}

    public static void sendError(Session session, String message) {
        session.send(errorMessage(message));
    }

    public static ServerMessage errorMessage(String message) {
        return new ServerMessage(OutgoingPackets.ERROR)
                .writeRaw(message == null ? "" : message);
    }

    public static void sendSuccess(Session session, int originatingOpcode) {
        session.send(new ServerMessage(OutgoingPackets.SUCCESS).writeInt(originatingOpcode));
    }

    public static void sendFailure(Session session, int originatingOpcode, String message) {
        session.send(new ServerMessage(OutgoingPackets.FAILURE)
                .writeInt(originatingOpcode)
                .writeString(message == null ? "" : message));
    }

    public static ServerMessage singleZeroMessage(int opcode) {
        return new ServerMessage(opcode).writeRaw(RAW_ZERO);
    }
}
