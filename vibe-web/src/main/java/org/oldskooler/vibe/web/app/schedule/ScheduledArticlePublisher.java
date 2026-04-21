package org.oldskooler.vibe.web.app.schedule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.vibe.web.app.event.CmsArticlePublishedEvent;
import org.oldskooler.vibe.web.app.event.WebEventBus;
import org.oldskooler.vibe.web.cms.article.ArticleService;
import org.oldskooler.vibe.web.cms.article.CmsArticle;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public final class ScheduledArticlePublisher {

    private static final Logger log = LogManager.getLogger(ScheduledArticlePublisher.class);

    private final ArticleService articleService;
    private final WebCronService cronService;
    private final WebEventBus eventBus;
    private final Duration interval;
    private ScheduledFuture<?> future;

    /**
     * Creates a new ScheduledArticlePublisher.
     * @param articleService the article service
     * @param cronService the cron service
     * @param eventBus the event bus
     * @param interval the polling interval
     */
    public ScheduledArticlePublisher(
            ArticleService articleService,
            WebCronService cronService,
            WebEventBus eventBus,
            Duration interval
    ) {
        this.articleService = articleService;
        this.cronService = cronService;
        this.eventBus = eventBus;
        this.interval = interval;
    }

    /**
     * Starts the publisher.
     */
    public synchronized void start() {
        if (future != null) {
            return;
        }

        publishDueArticles();
        future = cronService.schedule(interval, this::publishDueArticles);
    }

    /**
     * Stops the publisher.
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
        cronService.close();
    }

    private void publishDueArticles() {
        try {
            Timestamp now = Timestamp.from(Instant.now());
            List<CmsArticle> publishedArticles = articleService.publishDueScheduled(now);
            for (CmsArticle article : publishedArticles) {
                eventBus.publish(new CmsArticlePublishedEvent(article.id(), article.slug(), article.publishedAt()));
            }
        } catch (Exception e) {
            log.error("Failed to publish due scheduled articles", e);
        }
    }
}
