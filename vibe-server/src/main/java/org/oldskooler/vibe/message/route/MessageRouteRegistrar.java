package org.oldskooler.vibe.message.route;

import org.oldskooler.vibe.message.MessageRouter;

@FunctionalInterface
public interface MessageRouteRegistrar {
    /**
     * Registers.
     * @param router the router value
     */
    void register(MessageRouter router);
}
