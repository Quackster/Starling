package org.starling.identity;

import org.starling.contracts.AuthenticateResponse;
import org.starling.contracts.GetPlayerBootstrapRequest;
import org.starling.contracts.PlayerBootstrap;
import org.starling.contracts.PlayerBootstrapResponse;
import org.starling.contracts.PlayerData;
import org.starling.storage.entity.UserEntity;

import java.util.List;

/**
 * Maps persistence models into typed identity service contracts.
 */
public final class IdentityMappings {

    private static final List<String> DEFAULT_RIGHTS = List.of(
            "fuse_login",
            "fuse_buy_credits",
            "fuse_trade",
            "fuse_room_queue_default"
    );

    /**
     * Creates a new IdentityMappings.
     */
    private IdentityMappings() {}

    /**
     * Builds bootstrap.
     * @param user the user value
     * @return the result of this operation
     */
    public static PlayerBootstrap toBootstrap(UserEntity user) {
        return PlayerBootstrap.newBuilder()
                .setPlayer(toPlayerData(user))
                .addAllRights(DEFAULT_RIGHTS)
                .setSoundEnabled(user.getSoundEnabled() != 0)
                .setSoundVolume(0)
                .setAvailableBadgeCount(0)
                .setSelectedBadgeCount(0)
                .setPossibleAchievementCount(0)
                .build();
    }

    /**
     * Builds authentication failure.
     * @param message the message value
     * @return the result of this operation
     */
    public static AuthenticateResponse authenticationFailure(String message) {
        return AuthenticateResponse.newBuilder()
                .setAuthenticated(false)
                .setFailureMessage(message == null ? "" : message)
                .build();
    }

    /**
     * Builds bootstrap response.
     * @param user the user value
     * @return the result of this operation
     */
    public static PlayerBootstrapResponse bootstrapResponse(UserEntity user) {
        if (user == null) {
            return PlayerBootstrapResponse.newBuilder()
                    .setFound(false)
                    .build();
        }
        return PlayerBootstrapResponse.newBuilder()
                .setFound(true)
                .setBootstrap(toBootstrap(user))
                .build();
    }

    /**
     * Converts user to player data.
     * @param user the user value
     * @return the result of this operation
     */
    public static PlayerData toPlayerData(UserEntity user) {
        return PlayerData.newBuilder()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setFigure(user.getFigure())
                .setSex(user.getSex())
                .setMotto(user.getMotto())
                .setRank(user.getRank())
                .setSelectedRoomId(user.getSelectedRoomId())
                .setHomeRoom(user.getHomeRoom())
                .setCredits(user.getCredits())
                .build();
    }
}
