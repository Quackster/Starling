package org.starling.message.route;

import org.starling.message.IncomingPackets;
import org.starling.message.MessageRouter;
import org.starling.message.handler.HandshakeHandlers;

public final class HandshakeRouteRegistrar implements MessageRouteRegistrar {

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
