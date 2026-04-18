package org.starling.navigatorservice;

import org.starling.contracts.NavigatorCategory;
import org.starling.contracts.NavigatorNode;
import org.starling.contracts.NavigatePage;
import org.starling.contracts.Outcome;
import org.starling.contracts.OutcomeKind;
import org.starling.contracts.PlayerData;
import org.starling.contracts.PrivateRoomListing;
import org.starling.contracts.PublicRoomListing;
import org.starling.game.navigator.NavigatorManager;
import org.starling.storage.entity.NavigatorCategoryEntity;

import java.util.List;

/**
 * Maps navigator domain models into service contracts.
 */
public final class NavigatorContractMapper {

    public static final String ROOT_PUBLIC_CATEGORY_NAME = "Public Spaces";
    public static final String ROOT_PRIVATE_CATEGORY_NAME = "Rooms";

    /**
     * Creates a new NavigatorContractMapper.
     */
    private NavigatorContractMapper() {}

    /**
     * Converts category.
     * @param category the category value
     * @return the result of this operation
     */
    public static NavigatorCategory toCategory(NavigatorCategoryEntity category) {
        return NavigatorCategory.newBuilder()
                .setId(category.getId())
                .setParentId(category.getParentId())
                .setName(category.getName())
                .setMinRoleAccess(category.getMinRoleAccess())
                .setMinRoleSetFlatCat(category.getMinRoleSetFlatCat())
                .setFlatCategory(category.isFlatCategory())
                .setPublicCategory(category.isPublicCategory())
                .setOrderId(category.getOrderId())
                .build();
    }

    /**
     * Creates a synthetic root node.
     * @param categoryId the category id value
     * @return the result of this operation
     */
    public static NavigatorNode syntheticRoot(int categoryId) {
        String name = categoryId == 1 ? ROOT_PUBLIC_CATEGORY_NAME : ROOT_PRIVATE_CATEGORY_NAME;
        NavigatorCategory category = NavigatorCategory.newBuilder()
                .setId(categoryId)
                .setParentId(0)
                .setName(name)
                .setPublicCategory(categoryId == 1)
                .setFlatCategory(false)
                .build();
        return NavigatorNode.newBuilder()
                .setCategory(category)
                .setUserCount(0)
                .setCapacity(100)
                .build();
    }

    /**
     * Converts navigator node.
     * @param category the category value
     * @param rooms the rooms value
     * @return the result of this operation
     */
    public static NavigatorNode toNode(NavigatorCategoryEntity category, List<PrivateRoomListing> rooms) {
        int userCount = rooms.stream().mapToInt(PrivateRoomListing::getCurrentUsers).sum();
        int capacity = rooms.stream().mapToInt(PrivateRoomListing::getMaxUsers).sum();
        return NavigatorNode.newBuilder()
                .setCategory(toCategory(category))
                .setUserCount(userCount)
                .setCapacity(capacity > 0 ? capacity : (category.isFlatCategory() ? 100 : 0))
                .addAllPrivateRooms(rooms)
                .build();
    }

    /**
     * Creates outcome.
     * @param kind the kind value
     * @param message the message value
     * @return the result of this operation
     */
    public static Outcome outcome(OutcomeKind kind, String message) {
        return Outcome.newBuilder()
                .setKind(kind)
                .setMessage(message == null ? "" : message)
                .build();
    }

    /**
     * Returns the rank from player.
     * @param player the player value
     * @return the result of this operation
     */
    public static int rank(PlayerData player) {
        return player == null ? 1 : player.getRank();
    }

    /**
     * Returns the effective owner name.
     * @param viewer the viewer value
     * @param room the room value
     * @return the result of this operation
     */
    public static String visibleOwnerName(PlayerData viewer, PrivateRoomListing room) {
        if (room == null) {
            return "-";
        }
        if (room.getShowOwnerName() != 0) {
            return room.getOwnerName();
        }
        if (viewer != null && viewer.getId() != 0 && viewer.getId() == room.getOwnerId()) {
            return room.getOwnerName();
        }
        if (viewer != null && !viewer.getUsername().isBlank()
                && viewer.getUsername().equalsIgnoreCase(room.getOwnerName())) {
            return room.getOwnerName();
        }
        return "-";
    }

    /**
     * Filters full private rooms.
     * @param rooms the rooms value
     * @param hideFull the hide full value
     * @return the result of this operation
     */
    public static List<PrivateRoomListing> filterPrivateRooms(List<PrivateRoomListing> rooms, boolean hideFull) {
        return hideFull ? rooms.stream().filter(room -> room.getCurrentUsers() < room.getMaxUsers()).toList() : rooms;
    }

    /**
     * Filters full public rooms.
     * @param rooms the rooms value
     * @param hideFull the hide full value
     * @return the result of this operation
     */
    public static List<PublicRoomListing> filterPublicRooms(List<PublicRoomListing> rooms, boolean hideFull) {
        return hideFull ? rooms.stream().filter(room -> room.getCurrentUsers() < room.getMaxUsers()).toList() : rooms;
    }
}
