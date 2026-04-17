package org.starling.message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.message.route.MessageRouteRegistrar;
import org.starling.message.route.MessageRouteRegistrars;
import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;

import java.util.HashMap;
import java.util.Map;

public class MessageRouter {

    private static final Logger log = LogManager.getLogger(MessageRouter.class);

    private final Map<Integer, MessageHandler> handlers = new HashMap<>();

    public void register(int opcode, MessageHandler handler) {
        handlers.put(opcode, handler);
    }

    public void route(Session session, ClientMessage message) {
        MessageHandler handler = handlers.get(message.getOpcode());
        if (handler != null) {
            handler.handle(session, message);
        } else {
            log.debug("Unhandled opcode: {} [{}]", message.getOpcode(), message.headerString());
        }
    }

    /** Register all known packet handlers. */
    public void registerAll() {
        for (MessageRouteRegistrar registrar : MessageRouteRegistrars.defaults()) {
            registrar.register(this);
        }
    }
}
