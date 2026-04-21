package org.oldskooler.vibe.message.route;

import org.oldskooler.vibe.message.IncomingPackets;
import org.oldskooler.vibe.message.MessageRouter;
import org.oldskooler.vibe.message.handler.LoginHandlers;

public final class LoginRouteRegistrar implements MessageRouteRegistrar {

    /**
     * Registers.
     * @param router the router value
     */
    @Override
    public void register(MessageRouter router) {
        router.register(IncomingPackets.TRY_LOGIN, LoginHandlers::handleTryLogin);
        router.register(IncomingPackets.SSO, LoginHandlers::handleSSO);
        router.register(IncomingPackets.GET_INFO, LoginHandlers::handleGetInfo);
        router.register(IncomingPackets.GET_CREDITS, LoginHandlers::handleGetCredits);
        router.register(IncomingPackets.PONG, LoginHandlers::handlePong);
        router.register(IncomingPackets.GETAVAILABLEBADGES, LoginHandlers::handleGetAvailableBadges);
        router.register(IncomingPackets.GETSELECTEDBADGES, LoginHandlers::handleGetSelectedBadges);
        router.register(IncomingPackets.GET_SOUND_SETTING, LoginHandlers::handleGetSoundSetting);
        router.register(IncomingPackets.GET_POSSIBLE_ACHIEVEMENTS, LoginHandlers::handleGetPossibleAchievements);
    }
}
