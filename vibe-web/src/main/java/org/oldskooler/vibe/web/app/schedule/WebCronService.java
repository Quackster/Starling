package org.oldskooler.vibe.web.app.schedule;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class WebCronService implements AutoCloseable {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new CronThreadFactory());

    /**
     * Schedules a recurring job with the given fixed delay.
     * @param interval the interval between runs
     * @param job the job
     * @return the scheduled future
     */
    public ScheduledFuture<?> schedule(Duration interval, Runnable job) {
        long delayMillis = Math.max(1000L, interval.toMillis());
        return executor.scheduleWithFixedDelay(job, delayMillis, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private static final class CronThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "vibe-web-cron");
            thread.setDaemon(true);
            return thread;
        }
    }
}
