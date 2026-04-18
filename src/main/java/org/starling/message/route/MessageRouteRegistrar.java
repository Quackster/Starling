package org.starling.message.route;

import org.starling.message.MessageRouter;

@FunctionalInterface
public interface MessageRouteRegistrar {
    /**
     * Registers.
     * @param router the router value
     */
    void register(MessageRouter router);
}
