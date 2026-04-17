package org.starling.message.route;

import java.util.List;

public final class MessageRouteRegistrars {

    private static final List<MessageRouteRegistrar> DEFAULTS = List.of(
            new HandshakeRouteRegistrar(),
            new LoginRouteRegistrar(),
            new RoomRouteRegistrar(),
            new NavigatorRouteRegistrar()
    );

    private MessageRouteRegistrars() {}

    public static List<MessageRouteRegistrar> defaults() {
        return DEFAULTS;
    }
}
