package org.starling.web.feature.me.campaign;

import org.starling.storage.EntityContext;

import java.util.List;
import java.util.Optional;

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
        return EntityContext.withContext(context -> Math.toIntExact(context.from(CampaignEntity.class).count()));
    }

    /**
     * Returns all campaigns ordered for housekeeping.
     * @return the campaign list
     */
    public static List<HotCampaign> listAll() {
        return EntityContext.withContext(context -> context.from(CampaignEntity.class)
                .orderBy(order -> order
                        .col(CampaignEntity::getSortOrder).asc()
                        .col(CampaignEntity::getId).asc())
                .toList()
                .stream()
                .map(HotCampaignDao::map)
                .toList());
    }

    /**
     * Finds a campaign by id.
     * @param id the campaign id
     * @return the resulting campaign
     */
    public static Optional<HotCampaign> findById(int id) {
        return EntityContext.withContext(context -> context.from(CampaignEntity.class)
                .filter(filter -> filter.equals(CampaignEntity::getId, id))
                .first()
                .map(HotCampaignDao::map));
    }

    /**
     * Requires a campaign by id.
     * @param id the campaign id
     * @return the resulting campaign
     */
    public static HotCampaign require(int id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown campaign id " + id));
    }

    /**
     * Returns visible campaigns ordered for display.
     * @param limit the maximum number of campaigns
     * @return the campaign list
     */
    public static List<HotCampaign> listVisible(int limit) {
        return EntityContext.withContext(context -> {
            return context.from(CampaignEntity.class)
                    .filter(filter -> filter.equals(CampaignEntity::getVisible, 1))
                    .orderBy(order -> order
                            .col(CampaignEntity::getSortOrder).asc()
                            .col(CampaignEntity::getId).asc())
                    .limit(Math.max(limit, 0))
                    .toList()
                    .stream()
                    .map(HotCampaignDao::map)
                    .toList();
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
        save(null, url, imagePath, name, description, true, sortOrder);
    }

    /**
     * Creates or updates a campaign.
     * @param id the optional campaign id
     * @param url the campaign URL
     * @param imagePath the campaign image path
     * @param name the campaign name
     * @param description the campaign description
     * @param visible whether the campaign is visible
     * @param sortOrder the sort order
     * @return the resulting campaign id
     */
    public static int save(Integer id, String url, String imagePath, String name, String description, boolean visible, int sortOrder) {
        return EntityContext.inTransaction(context -> {
            CampaignEntity campaign = id == null
                    ? new CampaignEntity()
                    : context.from(CampaignEntity.class)
                    .filter(filter -> filter.equals(CampaignEntity::getId, id))
                    .first()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown campaign id " + id));

            campaign.setUrl(url);
            campaign.setImage(imagePath);
            campaign.setName(name);
            campaign.setDescription(description);
            campaign.setVisible(visible ? 1 : 0);
            campaign.setSortOrder(sortOrder);

            if (campaign.getId() > 0) {
                context.update(campaign);
            } else {
                context.insert(campaign);
            }

            return campaign.getId();
        });
    }

    /**
     * Deletes a campaign.
     * @param id the campaign id
     */
    public static void delete(int id) {
        EntityContext.inTransaction(context -> {
            context.from(CampaignEntity.class)
                    .filter(filter -> filter.equals(CampaignEntity::getId, id))
                    .delete();
            return null;
        });
    }

    /**
     * Returns the next default sort order.
     * @return the next sort order value
     */
    public static int nextSortOrder() {
        return listAll().stream()
                .mapToInt(HotCampaign::sortOrder)
                .max()
                .orElse(-1) + 1;
    }

    private static HotCampaign map(CampaignEntity campaign) {
        return new HotCampaign(
                campaign.getId(),
                campaign.getUrl(),
                campaign.getImage(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getVisible() > 0,
                campaign.getSortOrder()
        );
    }
}
