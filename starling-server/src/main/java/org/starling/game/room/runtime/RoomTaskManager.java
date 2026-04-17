package org.starling.game.room.runtime;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the periodic room task loop that advances walking occupants.
 */
public final class RoomTaskManager {

    private static final RoomTaskManager INSTANCE = new RoomTaskManager(RoomMovementService.getInstance());

    private final RoomMovementService roomMovementService;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    private RoomTaskManager(RoomMovementService roomMovementService) {
        this.roomMovementService = roomMovementService;
    }

    public static RoomTaskManager getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(new RoomTaskThreadFactory());
        scheduler.scheduleAtFixedRate(this::tickNow, 500, 500, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        started.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void tickNow() {
        roomMovementService.tickLoadedRooms();
    }

    /**
     * Creates the dedicated daemon thread that runs the room tick loop.
     */
    private static final class RoomTaskThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "starling-room-task");
            thread.setDaemon(true);
            return thread;
        }
    }
}
