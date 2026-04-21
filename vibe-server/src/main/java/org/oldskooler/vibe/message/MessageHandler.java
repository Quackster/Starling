package org.oldskooler.vibe.message;

import org.oldskooler.vibe.net.codec.ClientMessage;
import org.oldskooler.vibe.net.session.Session;

@FunctionalInterface
public interface MessageHandler {
    /**
     * Handles.
     * @param session the session value
     * @param message the message value
     */
    void handle(Session session, ClientMessage message);
}
