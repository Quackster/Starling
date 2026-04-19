package org.starling.web.cms.bootstrap.seed;

import org.starling.storage.dao.GroupDao;
import org.starling.storage.dao.PublicTagDao;
import org.starling.storage.dao.RecommendedItemDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.entity.GroupEntity;
import org.starling.storage.entity.RecommendedItemEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.UserEntity;
import org.starling.web.cms.bootstrap.seed.data.CmsCommunitySeedCatalog;
import org.starling.web.util.Slugifier;

import java.util.List;

public final class CmsCommunitySeedBootstrap {

    /**
     * Creates a new CmsCommunitySeedBootstrap.
     */
    private CmsCommunitySeedBootstrap() {}

    /**
     * Seeds bootstrap community data.
     * @param bootstrapUser the bootstrap user
     * @return the seeded community state
     */
    public static CommunitySeedState seed(UserEntity bootstrapUser) {
        List<RoomEntity> rooms = seedBootstrapRooms(bootstrapUser);
        List<GroupEntity> groups = seedBootstrapGroups(bootstrapUser, rooms);
        seedBootstrapRecommendedItems(rooms, groups);
        seedBootstrapTags(bootstrapUser, groups);
        return new CommunitySeedState(rooms, groups);
    }

    private static List<RoomEntity> seedBootstrapRooms(UserEntity bootstrapUser) {
        List<RoomEntity> existingRooms = RoomDao.findTopRated(12);
        if (!existingRooms.isEmpty()) {
            return existingRooms;
        }
        if (bootstrapUser == null) {
            return List.of();
        }

        for (CmsCommunitySeedCatalog.RoomSeed seed : CmsCommunitySeedCatalog.rooms()) {
            RoomEntity room = new RoomEntity();
            room.setCategoryId(1);
            room.setOwnerId(bootstrapUser.getId());
            room.setOwnerName(bootstrapUser.getUsername());
            room.setName(seed.name());
            room.setDescription(seed.description());
            room.setCurrentUsers(seed.currentUsers());
            room.setNavigatorFilter("popular");
            RoomDao.save(room);
        }

        return RoomDao.findTopRated(12);
    }

    private static List<GroupEntity> seedBootstrapGroups(UserEntity bootstrapUser, List<RoomEntity> bootstrapRooms) {
        List<GroupEntity> existingGroups = GroupDao.listAll();
        if (!existingGroups.isEmpty()) {
            return existingGroups;
        }
        if (bootstrapUser == null) {
            return List.of();
        }

        List<CmsCommunitySeedCatalog.GroupSeed> seeds = CmsCommunitySeedCatalog.groups();
        for (int index = 0; index < seeds.size(); index++) {
            CmsCommunitySeedCatalog.GroupSeed seed = seeds.get(index);
            GroupEntity group = new GroupEntity();
            group.setAlias(Slugifier.slugify(seed.name()));
            group.setName(seed.name());
            group.setBadge(seed.badge());
            group.setDescription(seed.description());
            group.setOwnerId(bootstrapUser.getId());
            if (!bootstrapRooms.isEmpty()) {
                group.setRoomId(bootstrapRooms.get(index % bootstrapRooms.size()).getId());
            }
            GroupDao.save(group);
            GroupDao.ensureMembership(bootstrapUser.getId(), group.getId(), 3, index == 0);
        }

        return GroupDao.listAll();
    }

    private static void seedBootstrapRecommendedItems(List<RoomEntity> bootstrapRooms, List<GroupEntity> bootstrapGroups) {
        if (RecommendedItemDao.listIds("room", null, 1).isEmpty()) {
            for (int index = 0; index < Math.min(4, bootstrapRooms.size()); index++) {
                RecommendedItemEntity item = new RecommendedItemEntity();
                item.setType("room");
                item.setRecId(bootstrapRooms.get(index).getId());
                item.setSponsored(0);
                RecommendedItemDao.save(item);
            }
        }

        if (RecommendedItemDao.listIds("group", null, 1).isEmpty()) {
            for (int index = 0; index < Math.min(4, bootstrapGroups.size()); index++) {
                RecommendedItemEntity item = new RecommendedItemEntity();
                item.setType("group");
                item.setRecId(bootstrapGroups.get(index).getId());
                item.setSponsored(1);
                RecommendedItemDao.save(item);
            }
        }
    }

    private static void seedBootstrapTags(UserEntity bootstrapUser, List<GroupEntity> bootstrapGroups) {
        if (bootstrapUser != null) {
            PublicTagDao.addTag("user", bootstrapUser.getId(), "retro");
            PublicTagDao.addTag("user", bootstrapUser.getId(), "builder");
            PublicTagDao.addTag("user", bootstrapUser.getId(), "community");
        }

        for (GroupEntity group : bootstrapGroups) {
            if (group.getName().contains("Welcome")) {
                PublicTagDao.addTag("group", group.getId(), "community");
                PublicTagDao.addTag("group", group.getId(), "newbies");
            } else if (group.getName().contains("Skyline")) {
                PublicTagDao.addTag("group", group.getId(), "social");
                PublicTagDao.addTag("group", group.getId(), "music");
            } else if (group.getName().contains("Builders")) {
                PublicTagDao.addTag("group", group.getId(), "builder");
                PublicTagDao.addTag("group", group.getId(), "design");
            } else if (group.getName().contains("Arcade")) {
                PublicTagDao.addTag("group", group.getId(), "games");
                PublicTagDao.addTag("group", group.getId(), "retro");
            } else if (group.getName().contains("Pool")) {
                PublicTagDao.addTag("group", group.getId(), "summer");
                PublicTagDao.addTag("group", group.getId(), "rooms");
            } else {
                PublicTagDao.addTag("group", group.getId(), "news");
                PublicTagDao.addTag("group", group.getId(), "events");
            }
        }
    }

    public record CommunitySeedState(List<RoomEntity> rooms, List<GroupEntity> groups) {}
}
