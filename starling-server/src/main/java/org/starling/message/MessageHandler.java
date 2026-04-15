package org.starling.message;

import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;

@FunctionalInterface
public interface MessageHandler {
    void handle(Session session, ClientMessage message);
}
