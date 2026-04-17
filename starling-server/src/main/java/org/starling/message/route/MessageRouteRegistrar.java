package org.starling.message.route;

import org.starling.message.MessageRouter;

@FunctionalInterface
public interface MessageRouteRegistrar {
    void register(MessageRouter router);
}
