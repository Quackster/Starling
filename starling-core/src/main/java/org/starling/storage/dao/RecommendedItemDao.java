package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.RecommendedItemEntity;

import java.sql.Timestamp;
import java.time.Instant;
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
        return EntityContext.withContext(context -> {
            var query = context.from(RecommendedItemEntity.class)
                    .filter(filter -> filter.equals(RecommendedItemEntity::getType, type))
                    .orderBy(order -> order.col(RecommendedItemEntity::getId).asc());

            if (sponsored != null) {
                query = query.filter(filter -> filter.equals(RecommendedItemEntity::getSponsored, sponsored ? 1 : 0));
            }
            if (limit > 0) {
                query = query.limit(limit);
            }

            return query.toList()
                    .stream()
                    .map(RecommendedItemEntity::getRecId)
                    .toList();
        });
    }
}
