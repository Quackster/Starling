package org.starling.game.player.auth;

import org.starling.game.player.Player;
import org.starling.message.OutgoingPackets;
import org.starling.message.support.HandlerResponses;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;
import org.starling.storage.entity.UserEntity;

public final class LoginResponseWriter {

    public void sendLoginFailure(Session session) {
        HandlerResponses.sendError(session, "login incorrect");
    }

    public Player sendLoginSuccess(Session session, UserEntity user) {
        Player player = new Player(user);
        session.setPlayer(player);

        session.send(new ServerMessage(OutgoingPackets.LOGIN_OK));
        session.send(buildUserRightsMessage());
        return player;
    }

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

    public void sendCreditBalance(Session session, Player player) {
        session.send(new ServerMessage(OutgoingPackets.CREDIT_BALANCE)
                .writeString(player.getCredits() + ".0"));
    }

    public void sendEmptyBadges(Session session) {
        ServerMessage response = new ServerMessage(OutgoingPackets.AVAILABLE_BADGES);
        response.writeInt(0);
        response.writeInt(0);
        response.writeBoolean(false);
        session.send(response);
    }

    public void sendSoundSetting(Session session) {
        session.send(new ServerMessage(OutgoingPackets.SOUND_SETTING)
                .writeBoolean(true)
                .writeInt(0));
    }

    public void sendPossibleAchievements(Session session) {
        session.send(new ServerMessage(OutgoingPackets.POSSIBLE_ACHIEVEMENTS).writeInt(0));
    }

    private ServerMessage buildUserRightsMessage() {
        ServerMessage rights = new ServerMessage(OutgoingPackets.USER_RIGHTS);
        rights.writeString("fuse_login");
        rights.writeString("fuse_buy_credits");
        rights.writeString("fuse_trade");
        rights.writeString("fuse_room_queue_default");
        return rights;
    }
}
