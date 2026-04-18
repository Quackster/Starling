package org.starling.web.view;

import org.starling.web.cms.model.CmsArticle;
import org.starling.web.cms.model.CmsMediaAsset;
import org.starling.web.cms.model.CmsNavigationItem;
import org.starling.web.cms.model.CmsNavigationMenu;
import org.starling.web.cms.model.CmsPage;
import org.starling.web.render.MarkdownRenderer;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CmsViewModelFactory {

    private final MarkdownRenderer markdownRenderer;

    /**
     * Creates a new CmsViewModelFactory.
     * @param markdownRenderer the markdown renderer
     */
    public CmsViewModelFactory(MarkdownRenderer markdownRenderer) {
        this.markdownRenderer = markdownRenderer;
    }

    /**
     * Creates a published page view model.
     * @param page the page value
     * @return the resulting view model
     */
    public Map<String, Object> page(CmsPage page) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", page.id());
        view.put("slug", page.slug());
        view.put("title", page.publishedTitle());
        view.put("summary", page.publishedSummary());
        view.put("markdown", page.publishedMarkdown());
        view.put("html", markdownRenderer.render(page.publishedMarkdown()));
        view.put("publishedAt", page.publishedAt());
        return view;
    }

    /**
     * Creates an article summary view model.
     * @param article the article value
     * @return the resulting view model
     */
    public Map<String, Object> articleSummary(CmsArticle article) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", article.id());
        view.put("slug", article.slug());
        view.put("title", article.published() ? article.publishedTitle() : article.draftTitle());
        view.put("summary", article.published() ? article.publishedSummary() : article.draftSummary());
        view.put("published", article.published());
        view.put("publishedAt", formatFriendlyDate(article.publishedAt()));
        view.put("createdAt", article.createdAt());
        view.put("updatedAt", article.updatedAt());
        view.put("url", "/news/" + article.slug());
        return view;
    }

    /**
     * Creates a detailed article view model.
     * @param article the article value
     * @return the resulting view model
     */
    public Map<String, Object> article(CmsArticle article) {
        Map<String, Object> view = articleSummary(article);
        view.put("markdown", article.publishedMarkdown());
        view.put("html", markdownRenderer.render(article.publishedMarkdown()));
        return view;
    }

    /**
     * Creates a Lisbon article view model.
     * @param article the article value
     * @return the resulting view model
     */
    public Map<String, Object> lisbonArticle(CmsArticle article) {
        if (article == null) {
            Map<String, Object> placeholder = new HashMap<>();
            placeholder.put("title", "No news");
            placeholder.put("shortstory", "There is no news.");
            placeholder.put("articleImage", "");
            placeholder.put("date", formatArticleDate(null));
            placeholder.put("escapedStory", "<p>There is no news.</p>");
            placeholder.put("author", "Starling CMS");
            placeholder.put("url", "no-news");
            placeholder.put("published", true);
            placeholder.put("categories", List.of());
            return placeholder;
        }

        Map<String, Object> view = new HashMap<>();
        view.put("title", article.publishedTitle());
        view.put("shortstory", article.publishedSummary());
        view.put("articleImage", "");
        view.put("date", formatArticleDate(article.publishedAt()));
        view.put("escapedStory", markdownRenderer.render(article.publishedMarkdown()));
        view.put("author", "Starling CMS");
        view.put("url", article.slug());
        view.put("published", article.published());
        view.put("categories", List.of());
        return view;
    }

    /**
     * Creates Lisbon archive buckets.
     * @param articles the article list
     * @return the resulting archive buckets
     */
    public Map<String, List<Map<String, Object>>> archiveBuckets(List<CmsArticle> articles) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        Map<String, List<Map<String, Object>>> buckets = new LinkedHashMap<>();

        for (CmsArticle article : articles) {
            if (article.publishedAt() == null) {
                continue;
            }

            String key = article.publishedAt().toInstant().atZone(ZoneId.systemDefault()).format(formatter);
            buckets.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(lisbonArticle(article));
        }

        return buckets;
    }

    /**
     * Creates a dated Lisbon bucket.
     * @param articles the article list
     * @param bucket the date bucket
     * @return the resulting article list
     */
    public List<Map<String, Object>> datedBucket(List<CmsArticle> articles, ArticleBucket bucket) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return articles.stream()
                .filter(article -> article.publishedAt() != null)
                .filter(article -> bucket.matches(
                        today,
                        article.publishedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                ))
                .map(this::lisbonArticle)
                .toList();
    }

    /**
     * Creates a blank featured article card.
     * @param index the display index
     * @return the placeholder article card
     */
    public Map<String, Object> emptyFeaturedArticle(int index) {
        Map<String, Object> view = new HashMap<>();
        view.put("title", "No news yet");
        view.put("summary", "Publish a CMS article to fill this slot.");
        view.put("date", "");
        view.put("url", "/news");
        view.put("image", "/web-gallery/v2/images/landing/uk_party_frontpage_image.gif");
        view.put("index", index);
        return view;
    }

    /**
     * Creates a page summary view model.
     * @param page the page value
     * @return the resulting view model
     */
    public Map<String, Object> pageSummary(CmsPage page) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", page.id());
        view.put("slug", page.slug());
        view.put("title", page.published() ? page.publishedTitle() : page.draftTitle());
        view.put("summary", page.published() ? page.publishedSummary() : page.draftSummary());
        view.put("templateName", page.templateName());
        view.put("published", page.published());
        view.put("publishedAt", page.publishedAt());
        view.put("updatedAt", page.updatedAt());
        return view;
    }

    /**
     * Creates a page editor view model.
     * @param page the page value
     * @return the resulting view model
     */
    public Map<String, Object> pageEditor(CmsPage page) {
        Map<String, Object> view = pageSummary(page);
        view.put("draftTitle", page.draftTitle());
        view.put("draftSummary", page.draftSummary());
        view.put("draftMarkdown", page.draftMarkdown());
        return view;
    }

    /**
     * Returns a blank page editor view model.
     * @return the resulting blank page model
     */
    public Map<String, Object> blankPage() {
        Map<String, Object> page = new HashMap<>();
        page.put("id", null);
        page.put("slug", "");
        page.put("title", "");
        page.put("summary", "");
        page.put("templateName", "page");
        page.put("draftTitle", "");
        page.put("draftSummary", "");
        page.put("draftMarkdown", "");
        page.put("published", false);
        page.put("publishedAt", null);
        page.put("updatedAt", null);
        return page;
    }

    /**
     * Creates an article editor view model.
     * @param article the article value
     * @return the resulting view model
     */
    public Map<String, Object> articleEditor(CmsArticle article) {
        Map<String, Object> view = articleSummary(article);
        view.put("draftTitle", article.draftTitle());
        view.put("draftSummary", article.draftSummary());
        view.put("draftMarkdown", article.draftMarkdown());
        return view;
    }

    /**
     * Returns a blank article editor view model.
     * @return the resulting blank article model
     */
    public Map<String, Object> blankArticle() {
        Map<String, Object> article = new HashMap<>();
        article.put("id", null);
        article.put("slug", "");
        article.put("title", "");
        article.put("summary", "");
        article.put("draftTitle", "");
        article.put("draftSummary", "");
        article.put("draftMarkdown", "");
        article.put("published", false);
        article.put("publishedAt", null);
        article.put("updatedAt", null);
        return article;
    }

    /**
     * Creates a menu view model.
     * @param menu the menu value
     * @return the resulting view model
     */
    public Map<String, Object> menu(CmsNavigationMenu menu) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", menu.id());
        view.put("menuKey", menu.menuKey());
        view.put("name", menu.name());
        return view;
    }

    /**
     * Creates a menu item view model.
     * @param item the item value
     * @return the resulting view model
     */
    public Map<String, Object> menuItem(CmsNavigationItem item) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", item.id());
        view.put("label", item.label());
        view.put("href", item.href());
        view.put("sortOrder", item.sortOrder());
        return view;
    }

    /**
     * Creates a media asset view model.
     * @param asset the asset value
     * @return the resulting view model
     */
    public Map<String, Object> mediaAsset(CmsMediaAsset asset) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", asset.id());
        view.put("fileName", asset.fileName());
        view.put("altText", asset.altText());
        return view;
    }

    private static String formatArticleDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE dd MMM, yyyy"));
    }

    private static String formatFriendlyDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    public enum ArticleBucket {
        TODAY {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.equals(today);
            }
        },
        YESTERDAY {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.equals(today.minusDays(1));
            }
        },
        THIS_WEEK {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.isBefore(today.minusDays(1)) && !publishedDate.isBefore(today.minusDays(7));
            }
        },
        THIS_MONTH {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.isBefore(today.minusDays(7)) && !publishedDate.isBefore(today.minusDays(30));
            }
        },
        PAST_YEAR {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.isBefore(today.minusDays(30));
            }
        };

        abstract boolean matches(LocalDate today, LocalDate publishedDate);
    }
}
