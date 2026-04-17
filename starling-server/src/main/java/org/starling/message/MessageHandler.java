package org.starling.message;

import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;

@FunctionalInterface
public interface MessageHandler {
    /**
     * Handles.
     * @param session the session value
     * @param message the message value
     */
    void handle(Session session, ClientMessage message);
}
