package org.starling.web.feature.credits.view;

import org.starling.storage.entity.UserEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CreditsPageContentFactory {

    /**
     * Returns the coin purchase categories.
     * @return the resulting category list
     */
    public List<Map<String, Object>> creditCategories() {
        return List.of(
                Map.of(
                        "id", "promo",
                        "title", "Best Way",
                        "headerClass", "credits-category-promo",
                        "methods", List.of(method(
                                44,
                                "Ask a Moderator",
                                "Moderators are all over the hotel. Ask one of them and they'll give you a voucher. Redeem it on the right.",
                                "<p><b>Here's How to do this:</b><br />Ask a staff member during a live event or a community giveaway.</p>",
                                false,
                                "",
                                "",
                                ""
                        ))
                ),
                Map.of(
                        "id", "quick_and_easy",
                        "title", "Other Ways",
                        "headerClass", "credits-category-quick_and_easy",
                        "methods", List.of(method(
                                1,
                                "Refer a Friend",
                                "Refer a friend to this hotel and earn some credits.",
                                "<p><b>How to do This:</b><br /><br />Invite friends to the hotel and reward them with a warm welcome once they arrive.</p>",
                                false,
                                "",
                                "",
                                ""
                        ))
                ),
                Map.of(
                        "id", "other",
                        "title", "Tools",
                        "headerClass", "credits-category-other",
                        "methods", List.of(method(
                                3,
                                "Reset Hand",
                                "Virtual hand too full? Click here to reset it.",
                                "<p><b>How to Do This:</b><br /><br />Use this helper when you want to clear your hand before jumping into the client.</p>",
                                true,
                                "Reset Hand",
                                "#",
                                "buy-button"
                        ))
                )
        );
    }

    /**
     * Returns the purse model.
     * @param currentUser the authenticated user, when present
     * @param resultMessage the redeem result message, when present
     * @return the resulting purse model
     */
    public Map<String, Object> purse(Optional<UserEntity> currentUser, String resultMessage) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("loggedIn", currentUser.isPresent());
        model.put("credits", currentUser.map(UserEntity::getCredits).orElse(0));
        model.put("resultMessage", resultMessage == null ? "" : resultMessage);
        return model;
    }

    /**
     * Returns the credits info box model.
     * @return the resulting info model
     */
    public Map<String, Object> creditsInfo() {
        return Map.of(
                "title", "What are Coins?",
                "text", "Coins let you buy furniture, unlock room decorations, and take part in the classic hotel economy.",
                "subtext", "Use the purchase methods on the left to learn how your hotel distributes them.",
                "image", "/web-gallery/v2/images/credits/poor.png"
        );
    }

    /**
     * Returns the pixels page panels.
     * @return the resulting panel list
     */
    public List<Map<String, Object>> pixelPanels() {
        return List.of(
                Map.of(
                        "className", "pixelblue",
                        "title", "Learn about Pixels",
                        "contentType", "info",
                        "heading", "You can earn Pixels:",
                        "listItems", List.of("Take part in events, community activities, and other hotel campaigns."),
                        "text", "Pixels are a soft currency for temporary effects, room rentals, and offers.",
                        "linkLabel", "Read the FAQ",
                        "linkHref", "/help"
                ),
                Map.of(
                        "className", "pixelgreen",
                        "title", "Rent some stuff",
                        "contentType", "image",
                        "image", "/web-gallery/v2/images/activitypoints/pixelpage_effectmachine.png",
                        "text", "Pixels can be exchanged for rentable items and community-focused extras."
                ),
                Map.of(
                        "className", "pixellightblue",
                        "title", "Effects",
                        "contentType", "image",
                        "image", "/web-gallery/v2/images/activitypoints/pixelpage_personaleffect.png",
                        "text", "Try temporary avatar effects and show off a different look around the hotel."
                ),
                Map.of(
                        "className", "pixeldarkblue",
                        "title", "Offers",
                        "contentType", "image",
                        "image", "/web-gallery/v2/images/activitypoints/pixelpage_discounts.png",
                        "text", "Keep an eye out for limited-time offers that help you stretch your Pixels further."
                )
        );
    }

    private Map<String, Object> method(
            int id,
            String title,
            String summary,
            String details,
            boolean showButton,
            String buttonLabel,
            String buttonHref,
            String buttonId
    ) {
        return Map.of(
                "id", id,
                "title", title,
                "summary", summary,
                "details", details,
                "showButton", showButton,
                "buttonLabel", buttonLabel,
                "buttonHref", buttonHref,
                "buttonId", buttonId
        );
    }
}
