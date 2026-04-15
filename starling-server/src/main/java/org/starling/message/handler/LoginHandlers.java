package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.Player;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ClientMessage;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;

public final class LoginHandlers {

    private static final Logger log = LogManager.getLogger(LoginHandlers.class);

    private LoginHandlers() {}

    /**
     * TRY_LOGIN (756) - Username/password login.
     * Params: B64Str username, B64Str password
     */
    public static void handleTryLogin(Session session, ClientMessage msg) {
        String username = msg.readString();
        String password = msg.readString();
        log.info("Login attempt: {}", username);

        UserEntity user = UserDao.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            ServerMessage error = new ServerMessage(OutgoingPackets.ERROR)
                    .writeRaw("login incorrect");
            session.send(error);
            log.warn("Login failed for: {}", username);
            return;
        }

        completeLogin(session, user);
    }

    /**
     * SSO (204) - Single Sign-On ticket login.
     * Params: B64Str ssoTicket
     */
    public static void handleSSO(Session session, ClientMessage msg) {
        String ticket = msg.readString();
        log.info("SSO login attempt with ticket: {}...", ticket.length() > 8 ? ticket.substring(0, 8) : ticket);

        UserEntity user = UserDao.findBySsoTicket(ticket);
        if (user == null) {
            ServerMessage error = new ServerMessage(OutgoingPackets.ERROR)
                    .writeRaw("login incorrect");
            session.send(error);
            log.warn("SSO login failed");
            return;
        }

        completeLogin(session, user);
    }

    private static void completeLogin(Session session, UserEntity user) {
        Player player = new Player(user);
        session.setPlayer(player);

        // LoginOK (3) - no params
        session.send(new ServerMessage(OutgoingPackets.LOGIN_OK));

        // UserRights (2) - series of 0x02-terminated privilege strings
        ServerMessage rights = new ServerMessage(OutgoingPackets.USER_RIGHTS);
        rights.writeString("fuse_login");
        rights.writeString("fuse_buy_credits");
        rights.writeString("fuse_trade");
        rights.writeString("fuse_room_queue_default");
        session.send(rights);

        log.info("User logged in: {} (id={})", user.getUsername(), user.getId());
    }

    /**
     * GET_INFO (7) - Client requests user object.
     * Respond with UserObj (5).
     */
    public static void handleGetInfo(Session session, ClientMessage msg) {
        Player player = session.getPlayer();
        if (player == null) return;

        ServerMessage response = new ServerMessage(OutgoingPackets.USER_OBJECT);
        response.writeString(String.valueOf(player.getId()));   // userId
        response.writeString(player.getUsername());              // name
        response.writeString(player.getFigure());               // figure
        response.writeString(player.getSex());                  // sex
        response.writeString(player.getMotto());                // customData
        response.writeInt(0);                                    // ph_tickets
        response.writeString("");                                // ph_figure
        response.writeInt(0);                                    // photo_film
        response.writeInt(0);                                    // directMail

        session.send(response);
    }

    /**
     * GET_CREDITS (8) - Client requests credit balance.
     * Respond with CreditBalance (6) as raw string "credits.0".
     */
    public static void handleGetCredits(Session session, ClientMessage msg) {
        Player player = session.getPlayer();
        if (player == null) return;

        ServerMessage response = new ServerMessage(OutgoingPackets.CREDIT_BALANCE)
                .writeString(player.getCredits() + ".0");
        session.send(response);
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
        ServerMessage response = new ServerMessage(OutgoingPackets.AVAILABLE_BADGES);
        response.writeInt(0); // badge count
        response.writeInt(0); // chosen badge index
        response.writeBoolean(false);
        session.send(response);
    }

    /** GETSELECTEDBADGES (159) - Same response as above. */
    public static void handleGetSelectedBadges(Session session, ClientMessage msg) {
        // Uses same response format; send empty
        ServerMessage response = new ServerMessage(OutgoingPackets.AVAILABLE_BADGES);
        response.writeInt(0);
        response.writeInt(0);
        response.writeBoolean(false);
        session.send(response);
    }

    /**
     * GET_SOUND_SETTING (228) - Respond with SoundSetting (308).
     */
    public static void handleGetSoundSetting(Session session, ClientMessage msg) {
        ServerMessage response = new ServerMessage(OutgoingPackets.SOUND_SETTING)
                .writeBoolean(true)
                .writeInt(0);
        session.send(response);
    }

    /**
     * GET_POSSIBLE_ACHIEVEMENTS (370) - Respond with PossibleAchievements (436).
     * Send empty list for now.
     */
    public static void handleGetPossibleAchievements(Session session, ClientMessage msg) {
        ServerMessage response = new ServerMessage(OutgoingPackets.POSSIBLE_ACHIEVEMENTS)
                .writeInt(0); // count
        session.send(response);
    }
}
