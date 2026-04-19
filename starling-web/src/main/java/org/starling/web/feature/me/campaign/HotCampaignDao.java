package org.starling.web.feature.me.campaign;

import org.starling.storage.EntityContext;

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
        return EntityContext.withContext(context -> Math.toIntExact(context.from(CampaignEntity.class).count()));
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
        EntityContext.inTransaction(context -> {
            CampaignEntity campaign = new CampaignEntity();
            campaign.setUrl(url);
            campaign.setImage(imagePath);
            campaign.setName(name);
            campaign.setDescription(description);
            campaign.setVisible(1);
            campaign.setSortOrder(sortOrder);
            context.insert(campaign);
            return null;
        });
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
