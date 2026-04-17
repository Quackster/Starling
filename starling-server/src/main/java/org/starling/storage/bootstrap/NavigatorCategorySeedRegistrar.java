package org.starling.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;

import java.sql.PreparedStatement;
import java.util.List;

public final class NavigatorCategorySeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(NavigatorCategorySeedRegistrar.class);
    private static final List<HolographPublicSpaceCatalog.NavigatorCategorySeed> DEFAULT_CATEGORIES =
            HolographPublicSpaceCatalog.load().navigatorCategories();

    @Override
    public void seed(DbContext context) {
        String sql = """
                INSERT INTO rooms_categories (
                    id,
                    order_id,
                    parent_id,
                    isnode,
                    name,
                    public_spaces,
                    allow_trading,
                    minrole_access,
                    minrole_setflatcat,
                    club_only,
                    is_top_priority
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    order_id = VALUES(order_id),
                    parent_id = VALUES(parent_id),
                    isnode = VALUES(isnode),
                    name = VALUES(name),
                    public_spaces = VALUES(public_spaces),
                    allow_trading = VALUES(allow_trading),
                    minrole_access = VALUES(minrole_access),
                    minrole_setflatcat = VALUES(minrole_setflatcat),
                    club_only = VALUES(club_only),
                    is_top_priority = VALUES(is_top_priority)
                """;

        try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
            for (HolographPublicSpaceCatalog.NavigatorCategorySeed seed : DEFAULT_CATEGORIES) {
                statement.setInt(1, seed.id());
                statement.setInt(2, seed.orderId());
                statement.setInt(3, seed.parentId());
                statement.setInt(4, seed.isNode());
                statement.setString(5, seed.name());
                statement.setInt(6, seed.publicSpaces());
                statement.setInt(7, seed.allowTrading());
                statement.setInt(8, seed.minRoleAccess());
                statement.setInt(9, seed.minRoleSetFlatCat());
                statement.setInt(10, seed.clubOnly());
                statement.setInt(11, seed.isTopPriority());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed navigator categories", e);
        }

        log.info("Ensured default navigator categories exist");
    }
}
