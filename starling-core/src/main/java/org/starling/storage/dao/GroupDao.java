package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.GroupEntity;
import org.starling.storage.entity.GroupMembershipEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GroupDao {

    private GroupDao() {}

    public static long count() {
        return EntityContext.withContext(context -> context.from(GroupEntity.class).count());
    }

    public static List<GroupEntity> listAll() {
        return EntityContext.withContext(context -> context.from(GroupEntity.class)
                .orderBy(order -> order
                        .col(GroupEntity::getName).asc()
                        .col(GroupEntity::getId).asc())
                .toList());
    }

    public static GroupEntity findById(int id) {
        return EntityContext.withContext(context -> context.from(GroupEntity.class)
                .filter(filter -> filter.equals(GroupEntity::getId, id))
                .first()
                .orElse(null));
    }

    public static GroupEntity findByAlias(String alias) {
        return EntityContext.withContext(context -> context.from(GroupEntity.class)
                .filter(filter -> filter.equalsIgnoreCase(GroupEntity::getAlias, alias == null ? "" : alias.trim()))
                .first()
                .orElse(null));
    }

    public static List<GroupEntity> listByUserId(int userId) {
        return findByIdsInOrder(EntityContext.withContext(context -> context.from(GroupMembershipEntity.class)
                .filter(filter -> filter
                        .equals(GroupMembershipEntity::getUserId, userId)
                        .and()
                        .equals(GroupMembershipEntity::getIsPending, 0))
                .orderBy(order -> order
                        .col(GroupMembershipEntity::getIsCurrent).desc()
                        .col(GroupMembershipEntity::getGroupId).asc())
                .toList()
                .stream()
                .map(GroupMembershipEntity::getGroupId)
                .toList()));
    }

    public static List<GroupEntity> listHot(int limit) {
        return EntityContext.withContext(context -> {
            List<GroupEntity> groups = context.from(GroupEntity.class).toList();
            Map<Integer, Integer> memberCounts = new HashMap<>();

            for (GroupMembershipEntity membership : context.from(GroupMembershipEntity.class)
                    .filter(filter -> filter.equals(GroupMembershipEntity::getIsPending, 0))
                    .toList()) {
                memberCounts.merge(membership.getGroupId(), 1, Integer::sum);
            }

            return groups.stream()
                    .sorted(Comparator
                            .comparingInt((GroupEntity group) -> memberCounts.getOrDefault(group.getId(), 0))
                            .reversed()
                            .thenComparingInt(GroupEntity::getId))
                    .limit(limit > 0 ? limit : groups.size())
                    .toList();
        });
    }

    public static List<GroupEntity> findRecommended(Boolean sponsored, int limit) {
        List<Integer> ids = RecommendedItemDao.listIds("group", sponsored, limit);
        if (ids.isEmpty()) {
            return listHot(limit);
        }
        return findByIdsInOrder(ids);
    }

    public static int countMembers(int groupId) {
        return EntityContext.withContext(context -> Math.toIntExact(context.from(GroupMembershipEntity.class)
                .filter(filter -> filter
                        .equals(GroupMembershipEntity::getGroupId, groupId)
                        .and()
                        .equals(GroupMembershipEntity::getIsPending, 0))
                .count()));
    }

    public static List<Integer> listMemberIds(int groupId, int limit) {
        return EntityContext.withContext(context -> {
            var memberQuery = context.from(GroupMembershipEntity.class)
                    .filter(filter -> filter
                            .equals(GroupMembershipEntity::getGroupId, groupId)
                            .and()
                            .equals(GroupMembershipEntity::getIsPending, 0))
                    .orderBy(order -> order
                            .col(GroupMembershipEntity::getIsCurrent).desc()
                            .col(GroupMembershipEntity::getMemberRank).desc()
                            .col(GroupMembershipEntity::getUserId).asc());

            if (limit > 0) {
                memberQuery = memberQuery.limit(limit);
            }

            return memberQuery.toList()
                    .stream()
                    .map(GroupMembershipEntity::getUserId)
                    .toList();
        });
    }

    public static GroupEntity save(GroupEntity group) {
        Timestamp now = Timestamp.from(Instant.now());
        if (group.getCreatedAt() == null) {
            group.setCreatedAt(now);
        }
        group.setUpdatedAt(now);

        return EntityContext.inTransaction(context -> {
            if (group.getId() > 0) {
                context.update(group);
            } else {
                context.insert(group);
            }
            return group;
        });
    }

    public static GroupMembershipEntity findMembership(int userId, int groupId) {
        return EntityContext.withContext(context -> context.from(GroupMembershipEntity.class)
                .filter(filter -> filter
                        .equals(GroupMembershipEntity::getUserId, userId)
                        .equals(GroupMembershipEntity::getGroupId, groupId))
                .first()
                .orElse(null));
    }

    public static GroupMembershipEntity saveMembership(GroupMembershipEntity membership) {
        if (membership.getCreatedAt() == null) {
            membership.setCreatedAt(Timestamp.from(Instant.now()));
        }

        return EntityContext.inTransaction(context -> {
            if (membership.getId() > 0) {
                context.update(membership);
            } else {
                context.insert(membership);
            }
            return membership;
        });
    }

    public static GroupMembershipEntity ensureMembership(int userId, int groupId, int memberRank, boolean current) {
        GroupMembershipEntity membership = findMembership(userId, groupId);
        if (membership == null) {
            membership = new GroupMembershipEntity();
            membership.setUserId(userId);
            membership.setGroupId(groupId);
            membership.setMemberRank(memberRank);
            membership.setIsCurrent(current ? 1 : 0);
            membership.setIsPending(0);
            return saveMembership(membership);
        }

        membership.setMemberRank(Math.max(membership.getMemberRank(), memberRank));
        membership.setIsCurrent(current ? 1 : membership.getIsCurrent());
        membership.setIsPending(0);
        return saveMembership(membership);
    }

    private static List<GroupEntity> findByIdsInOrder(List<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }

        List<GroupEntity> groups = EntityContext.withContext(context -> context.from(GroupEntity.class)
                .filter(filter -> filter.in(GroupEntity::getId, groupIds))
                .toList());

        Map<Integer, GroupEntity> byId = new LinkedHashMap<>();
        for (GroupEntity group : groups) {
            byId.put(group.getId(), group);
        }

        List<GroupEntity> ordered = new ArrayList<>();
        for (Integer groupId : groupIds) {
            GroupEntity group = byId.get(groupId);
            if (group != null) {
                ordered.add(group);
            }
        }
        return ordered;
    }

}
