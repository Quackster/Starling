package org.starling.web.feature.me.campaign;

import org.starling.storage.EntityContext;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class HotCampaignDao {

    /**
     * Creates a new HotCampaignDao.
     */
    private HotCampaignDao() {}

    /**
     * Returns the total campaign count.
     * @return the campaign count
     */
    public static int count() {
        return EntityContext.withContext(context -> {
            try (var statement = context.conn().prepareStatement("SELECT COUNT(*) FROM campaigns");
                 var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            } catch (Exception e) {
                throw new RuntimeException("Failed to count hot campaigns", e);
            }
        });
    }

    /**
     * Returns visible campaigns ordered for display.
     * @param limit the maximum number of campaigns
     * @return the campaign list
     */
    public static List<HotCampaign> listVisible(int limit) {
        return EntityContext.withContext(context -> {
            List<HotCampaign> campaigns = new ArrayList<>();
            try (var statement = context.conn().prepareStatement("""
                    SELECT id, url, image, name, `desc` AS description, visible, sort_order
                    FROM campaigns
                    WHERE visible = 1
                    ORDER BY sort_order ASC, id ASC
                    LIMIT ?
                    """)) {
                statement.setInt(1, Math.max(limit, 0));
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        campaigns.add(map(resultSet));
                    }
                }
                return campaigns;
            } catch (Exception e) {
                throw new RuntimeException("Failed to list hot campaigns", e);
            }
        });
    }

    /**
     * Creates a new campaign row.
     * @param url the campaign URL
     * @param imagePath the campaign image path
     * @param name the campaign name
     * @param description the campaign description
     * @param sortOrder the sort order
     */
    public static void create(String url, String imagePath, String name, String description, int sortOrder) {
        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    INSERT INTO campaigns (url, image, name, `desc`, visible, sort_order)
                    VALUES (?, ?, ?, ?, 1, ?)
                    """)) {
                statement.setString(1, url);
                statement.setString(2, imagePath);
                statement.setString(3, name);
                statement.setString(4, description);
                statement.setInt(5, sortOrder);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create hot campaign", e);
            }
        });
    }

    private static HotCampaign map(ResultSet resultSet) throws Exception {
        return new HotCampaign(
                resultSet.getInt("id"),
                resultSet.getString("url"),
                resultSet.getString("image"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("visible") > 0,
                resultSet.getInt("sort_order")
        );
    }
}
