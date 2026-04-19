package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.GroupEntity;
import org.starling.storage.entity.GroupMembershipEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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
        return findByIdsInOrder(queryIds(
                """
                SELECT groupid
                FROM groups_memberships
                WHERE userid = ? AND is_pending = 0
                ORDER BY is_current DESC, groupid ASC
                """,
                statement -> statement.setInt(1, userId)
        ));
    }

    public static List<GroupEntity> listHot(int limit) {
        String sql = """
                SELECT g.id
                FROM groups_details g
                LEFT JOIN groups_memberships m ON m.groupid = g.id AND m.is_pending = 0
                GROUP BY g.id
                ORDER BY COUNT(m.id) DESC, g.id ASC
                """ + (limit > 0 ? " LIMIT ?" : "");

        return findByIdsInOrder(queryIds(sql, statement -> {
            if (limit > 0) {
                statement.setInt(1, limit);
            }
        }));
    }

    public static List<GroupEntity> findRecommended(Boolean sponsored, int limit) {
        List<Integer> ids = RecommendedItemDao.listIds("group", sponsored, limit);
        if (ids.isEmpty()) {
            return listHot(limit);
        }
        return findByIdsInOrder(ids);
    }

    public static int countMembers(int groupId) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "SELECT COUNT(*) FROM groups_memberships WHERE groupid = ? AND is_pending = 0"
            )) {
                statement.setInt(1, groupId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to count group members", e);
            }
        });
    }

    public static List<Integer> listMemberIds(int groupId, int limit) {
        String sql = """
                SELECT userid
                FROM groups_memberships
                WHERE groupid = ? AND is_pending = 0
                ORDER BY is_current DESC, member_rank DESC, userid ASC
                """ + (limit > 0 ? " LIMIT ?" : "");

        return queryIds(sql, statement -> {
            statement.setInt(1, groupId);
            if (limit > 0) {
                statement.setInt(2, limit);
            }
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

    private static List<Integer> queryIds(String sql, SqlBinder binder) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
                if (binder != null) {
                    binder.bind(statement);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Integer> ids = new ArrayList<>();
                    while (resultSet.next()) {
                        ids.add(resultSet.getInt(1));
                    }
                    return ids;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to query group ids", e);
            }
        });
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

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws Exception;
    }
}
