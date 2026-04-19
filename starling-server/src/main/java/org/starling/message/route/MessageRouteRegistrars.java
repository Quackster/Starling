package org.starling.message.route;

import java.util.List;

public final class MessageRouteRegistrars {

    private static final List<MessageRouteRegistrar> DEFAULTS = List.of(
            new HandshakeRouteRegistrar(),
            new LoginRouteRegistrar(),
            new MessengerRouteRegistrar(),
            new RoomRouteRegistrar(),
            new NavigatorRouteRegistrar()
    );

    /**
     * Creates a new MessageRouteRegistrars.
     */
    private MessageRouteRegistrars() {}

    /**
     * Defaultses.
     * @return the resulting defaults
     */
    public static List<MessageRouteRegistrar> defaults() {
        return DEFAULTS;
    }
}
