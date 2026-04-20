package org.starling.web.cms.article;

import org.starling.web.render.MarkdownRenderer;
import org.starling.web.site.SiteBranding;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArticleViewFactory {

    private static final String TOP_STORY_IMAGE_BASE = "https://sandbox.h4bbo.net/c_images/Top_Story_Images/";
    private static final Map<String, String> TOP_STORY_IMAGES = Map.of(
            "welcome-to-starling", "TS_buildhotel.gif",
            "build-hotel-weekend", "Rel22_tstory_wide_300x187.gif",
            "library-lounge-now-open", "topStory_yearBook.gif",
            "dragon-quest-launch", "TS_Merdragon_Relocate_v1.gif",
            "neon-dj-takeover", "Neon_TS_DJ_300x187_v1.gif"
    );
    private static final List<String> TOP_STORY_FALLBACKS = List.of(
            "buildhotel.png",
            "Topstory_LIBRARY.png",
            "dragon_TP_1.gif",
            "Neon_TS_DJ_300x187_v1.gif",
            "TS_SUMMER_Party_Music_Dancing.gif",
            "Rel22_tstory_wide_300x187.gif"
    );

    private final MarkdownRenderer markdownRenderer;
    private final SiteBranding siteBranding;

    /**
     * Creates a new ArticleViewFactory.
     * @param markdownRenderer the markdown renderer
     */
    public ArticleViewFactory(MarkdownRenderer markdownRenderer, SiteBranding siteBranding) {
        this.markdownRenderer = markdownRenderer;
        this.siteBranding = siteBranding;
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
        view.put("title", article.title());
        view.put("summary", article.summary());
        view.put("published", article.published());
        view.put("scheduledPublishAt", formatEditorDateTime(article.scheduledPublishAt()));
        view.put("publishedAt", formatFriendlyDate(article.publishedAt()));
        view.put("createdAt", article.createdAt());
        view.put("updatedAt", article.updatedAt());
        view.put("url", "/news/" + article.slug());
        return view;
    }

    /**
     * Creates a news promo article view model.
     * @param article the article value
     * @return the resulting promo model
     */
    public Map<String, Object> newsPromoArticle(CmsArticle article) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("title", article.title());
        view.put("summary", article.summary());
        view.put("url", "/articles/" + article.slug());
        view.put("date", formatFriendlyDate(article.publishedAt()));
        view.put("image", topStoryImageUrl(article));
        return view;
    }

    /**
     * Returns a placeholder news promo article.
     * @return the resulting placeholder article
     */
    public Map<String, Object> emptyNewsPromoArticle() {
        return Map.of(
                "title", "No news yet",
                "summary", "Publish a CMS article to fill this slot.",
                "url", "/articles",
                "date", "",
                "image", TOP_STORY_IMAGE_BASE + TOP_STORY_FALLBACKS.get(0)
        );
    }

    /**
     * Creates a detailed article view model.
     * @param article the article value
     * @return the resulting view model
     */
    public Map<String, Object> article(CmsArticle article) {
        Map<String, Object> view = articleSummary(article);
        view.put("markdown", article.markdown());
        view.put("html", markdownRenderer.render(article.markdown()));
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
            placeholder.put("author", siteBranding.cmsTitle());
            placeholder.put("url", "no-news");
            placeholder.put("published", true);
            placeholder.put("categories", List.of());
            return placeholder;
        }

        Map<String, Object> view = new HashMap<>();
        view.put("title", article.title());
        view.put("shortstory", article.summary());
        view.put("articleImage", topStoryImageUrl(article));
        view.put("date", formatArticleDate(article.publishedAt()));
        view.put("escapedStory", markdownRenderer.render(article.markdown()));
        view.put("author", siteBranding.cmsTitle());
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
    public List<Map<String, Object>> datedBucket(List<CmsArticle> articles, ArchiveBucket bucket) {
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
     * Creates an article editor view model.
     * @param article the article value
     * @return the resulting view model
     */
    public Map<String, Object> articleEditor(CmsArticle article) {
        Map<String, Object> view = articleSummary(article);
        view.put("draftTitle", article.title());
        view.put("draftSummary", article.summary());
        view.put("draftMarkdown", article.markdown());
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
        article.put("scheduledPublishAt", "");
        article.put("published", false);
        article.put("publishedAt", null);
        article.put("updatedAt", null);
        return article;
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

    private static String formatEditorDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    private String topStoryImageUrl(CmsArticle article) {
        String fileName = TOP_STORY_IMAGES.get(article.slug());
        if (fileName == null) {
            int fallbackIndex = Math.floorMod(article.slug().hashCode(), TOP_STORY_FALLBACKS.size());
            fileName = TOP_STORY_FALLBACKS.get(fallbackIndex);
        }
        return TOP_STORY_IMAGE_BASE + fileName;
    }

    public enum ArchiveBucket {
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
