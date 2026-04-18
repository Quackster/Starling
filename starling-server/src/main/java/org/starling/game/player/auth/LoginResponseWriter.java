package org.starling.game.player.auth;

import org.starling.game.player.Player;
import org.starling.message.OutgoingPackets;
import org.starling.message.support.HandlerResponses;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;
import org.starling.storage.entity.UserEntity;

import java.util.List;

/**
 * Sends login and initial account bootstrap packets.
 */
public final class LoginResponseWriter {

    /**
     * Sends login failure.
     * @param session the session value
     */
    public void sendLoginFailure(Session session) {
        HandlerResponses.sendError(session, "login incorrect");
    }

    /**
     * Sends login success.
     * @param session the session value
     * @param user the user value
     */
    public void sendLoginSuccess(Session session, UserEntity user) {
        Player player = new Player(user);
        sendLoginSuccess(session, player, List.of(
                "fuse_login",
                "fuse_buy_credits",
                "fuse_trade",
                "fuse_room_queue_default"
        ));
    }

    /**
     * Sends login success.
     * @param session the session value
     * @param player the player value
     * @param rights the rights value
     */
    public void sendLoginSuccess(Session session, Player player, List<String> rights) {
        session.setPlayer(player);

        session.send(new ServerMessage(OutgoingPackets.LOGIN_OK));
        session.send(buildUserRightsMessage(rights));
    }

    /**
     * Sends user object.
     * @param session the session value
     * @param player the player value
     */
    public void sendUserObject(Session session, Player player) {
        ServerMessage response = new ServerMessage(OutgoingPackets.USER_OBJECT);
        response.writeString(String.valueOf(player.getId()));
        response.writeString(player.getUsername());
        response.writeString(player.getFigure());
        response.writeString(player.getSex());
        response.writeString(player.getMotto());
        response.writeInt(0);
        response.writeString("");
        response.writeInt(0);
        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends credit balance.
     * @param session the session value
     * @param player the player value
     */
    public void sendCreditBalance(Session session, Player player) {
        session.send(new ServerMessage(OutgoingPackets.CREDIT_BALANCE)
                .writeString(player.getCredits() + ".0"));
    }

    /**
     * Sends empty badges.
     * @param session the session value
     */
    public void sendEmptyBadges(Session session) {
        ServerMessage response = new ServerMessage(OutgoingPackets.AVAILABLE_BADGES);
        response.writeInt(0);
        response.writeInt(0);
        response.writeBoolean(false);
        session.send(response);
    }

    /**
     * Sends sound setting.
     * @param session the session value
     */
    public void sendSoundSetting(Session session) {
        session.send(new ServerMessage(OutgoingPackets.SOUND_SETTING)
                .writeBoolean(true)
                .writeInt(0));
    }

    /**
     * Sends possible achievements.
     * @param session the session value
     */
    public void sendPossibleAchievements(Session session) {
        session.send(new ServerMessage(OutgoingPackets.POSSIBLE_ACHIEVEMENTS).writeInt(0));
    }

    /**
     * Builds user rights message.
     * @return the resulting build user rights message
     */
    private ServerMessage buildUserRightsMessage(List<String> rights) {
        ServerMessage message = new ServerMessage(OutgoingPackets.USER_RIGHTS);
        for (String value : rights) {
            message.writeString(value);
        }
        return message;
    }
}
