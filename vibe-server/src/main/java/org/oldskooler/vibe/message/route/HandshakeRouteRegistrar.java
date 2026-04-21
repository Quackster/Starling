package org.oldskooler.vibe.message.route;

import org.oldskooler.vibe.message.IncomingPackets;
import org.oldskooler.vibe.message.MessageRouter;
import org.oldskooler.vibe.message.handler.HandshakeHandlers;

public final class HandshakeRouteRegistrar implements MessageRouteRegistrar {

    /**
     * Registers.
     * @param router the router value
     */
    @Override
    public void register(MessageRouter router) {
        router.register(IncomingPackets.INIT_CRYPTO, HandshakeHandlers::handleInitCrypto);
        router.register(IncomingPackets.GENERATEKEY, HandshakeHandlers::handleGenerateKey);
        router.register(IncomingPackets.SECRETKEY, HandshakeHandlers::handleSecretKey);
        router.register(IncomingPackets.VERSIONCHECK, HandshakeHandlers::handleVersionCheck);
        router.register(IncomingPackets.UNIQUEID, HandshakeHandlers::handleUniqueId);
        router.register(IncomingPackets.GET_SESSION_PARAMETERS, HandshakeHandlers::handleGetSessionParameters);
    }
}
