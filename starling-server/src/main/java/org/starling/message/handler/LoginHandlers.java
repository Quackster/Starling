package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.player.Player;
import org.starling.game.player.auth.LoginResponseWriter;
import org.starling.game.player.auth.LoginService;
import org.starling.message.support.SessionGuards;
import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;

public final class LoginHandlers {

    private static final Logger log = LogManager.getLogger(LoginHandlers.class);
    private static final LoginResponseWriter responses = new LoginResponseWriter();
    private static final LoginService loginService = new LoginService(responses);

    private LoginHandlers() {}

    /**
     * TRY_LOGIN (756) - Username/password login.
     * Params: B64Str username, B64Str password
     */
    public static void handleTryLogin(Session session, ClientMessage msg) {
        String username = msg.readString();
        String password = msg.readString();
        loginService.tryLogin(session, username, password);
    }

    /**
     * SSO (204) - Single Sign-On ticket login.
     * Params: B64Str ssoTicket
     */
    public static void handleSSO(Session session, ClientMessage msg) {
        String ticket = msg.readString();
        loginService.loginWithTicket(session, ticket);
    }

    /**
     * GET_INFO (7) - Client requests user object.
     * Respond with UserObj (5).
     */
    public static void handleGetInfo(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "user info");
        if (player == null) {
            return;
        }
        responses.sendUserObject(session, player);
    }

    /**
     * GET_CREDITS (8) - Client requests credit balance.
     * Respond with CreditBalance (6) as raw string "credits.0".
     */
    public static void handleGetCredits(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "credits");
        if (player == null) {
            return;
        }
        responses.sendCreditBalance(session, player);
    }

    /** PONG (196) - Client keepalive response. No action needed. */
    public static void handlePong(Session session, ClientMessage msg) {
        // Client is alive
    }

    /**
     * GETAVAILABLEBADGES (157) - Respond with AvailableBadges (229).
     * Send empty badge lists for now.
     */
    public static void handleGetAvailableBadges(Session session, ClientMessage msg) {
        responses.sendEmptyBadges(session);
    }

    /** GETSELECTEDBADGES (159) - Same response as above. */
    public static void handleGetSelectedBadges(Session session, ClientMessage msg) {
        responses.sendEmptyBadges(session);
    }

    /**
     * GET_SOUND_SETTING (228) - Respond with SoundSetting (308).
     */
    public static void handleGetSoundSetting(Session session, ClientMessage msg) {
        responses.sendSoundSetting(session);
    }

    /**
     * GET_POSSIBLE_ACHIEVEMENTS (370) - Respond with PossibleAchievements (436).
     * Send empty list for now.
     */
    public static void handleGetPossibleAchievements(Session session, ClientMessage msg) {
        responses.sendPossibleAchievements(session);
    }
}
