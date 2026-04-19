package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.RecommendedItemEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RecommendedItemDao {

    private RecommendedItemDao() {}

    public static long count() {
        return EntityContext.withContext(context -> context.from(RecommendedItemEntity.class).count());
    }

    public static RecommendedItemEntity save(RecommendedItemEntity item) {
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(Timestamp.from(Instant.now()));
        }

        return EntityContext.inTransaction(context -> {
            if (item.getId() > 0) {
                context.update(item);
            } else {
                context.insert(item);
            }
            return item;
        });
    }

    public static List<Integer> listIds(String type, Boolean sponsored, int limit) {
        StringBuilder sql = new StringBuilder("SELECT rec_id FROM recommended WHERE type = ?");
        if (sponsored != null) {
            sql.append(" AND sponsored = ?");
        }
        sql.append(" ORDER BY id ASC");
        if (limit > 0) {
            sql.append(" LIMIT ?");
        }

        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql.toString())) {
                int index = 1;
                statement.setString(index++, type);
                if (sponsored != null) {
                    statement.setInt(index++, sponsored ? 1 : 0);
                }
                if (limit > 0) {
                    statement.setInt(index, limit);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Integer> ids = new ArrayList<>();
                    while (resultSet.next()) {
                        ids.add(resultSet.getInt(1));
                    }
                    return ids;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load recommended ids for " + type, e);
            }
        });
    }
}
