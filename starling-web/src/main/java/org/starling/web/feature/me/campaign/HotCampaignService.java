package org.starling.web.service;

import org.starling.web.me.HotCampaign;
import org.starling.web.me.HotCampaignDao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HotCampaignService {

    /**
     * Returns visible hot campaigns for the /me page.
     * @return the campaign view models
     */
    public List<Map<String, Object>> listVisible() {
        return HotCampaignDao.listVisible(5).stream()
                .map(this::toViewModel)
                .toList();
    }

    private Map<String, Object> toViewModel(HotCampaign campaign) {
        Map<String, Object> viewModel = new LinkedHashMap<>();
        viewModel.put("id", campaign.id());
        viewModel.put("url", campaign.url());
        viewModel.put("imageUrl", campaign.imagePath());
        viewModel.put("name", campaign.name());
        viewModel.put("description", campaign.description());
        return viewModel;
    }
}
