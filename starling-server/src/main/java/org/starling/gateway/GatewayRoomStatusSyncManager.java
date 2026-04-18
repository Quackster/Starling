package org.starling.gateway;

import org.starling.contracts.RoomOccupant;
import org.starling.contracts.RoomSnapshot;
import org.starling.contracts.RoomSnapshotResult;
import org.starling.contracts.RoomType;
import org.starling.gateway.rpc.RoomClient;
import org.starling.game.player.PlayerManager;
import org.starling.game.room.response.RoomResponseWriter;
import org.starling.net.session.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls room-service snapshots for active rooms and rebroadcasts status when
 * occupant movement changes.
 */
public final class GatewayRoomStatusSyncManager {

    private static final long POLL_INTERVAL_MS = 100L;

    private final RoomClient roomClient;
    private final RoomResponseWriter responses;
    private final Map<String, String> lastStatusByRoom = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    /**
     * Creates a new GatewayRoomStatusSyncManager.
     * @param roomClient the room client value
     * @param responses the responses value
     */
    public GatewayRoomStatusSyncManager(RoomClient roomClient, RoomResponseWriter responses) {
        this.roomClient = roomClient;
        this.responses = responses;
    }

    /**
     * Starts the sync loop.
     */
    public void start() {
        if (roomClient == null || !started.compareAndSet(false, true)) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(new RoomSyncThreadFactory());
        scheduler.scheduleAtFixedRate(this::tickNow, POLL_INTERVAL_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the sync loop.
     */
    public void stop() {
        started.set(false);
        lastStatusByRoom.clear();
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /**
     * Ticks once.
     */
    public void tickNow() {
        Map<String, Session> anchors = activeRoomAnchors();
        lastStatusByRoom.keySet().removeIf(key -> !anchors.containsKey(key));

        for (Map.Entry<String, Session> entry : anchors.entrySet()) {
            RoomSnapshotResult result = roomClient.getRoomSnapshot(entry.getValue().getSessionId());
            if (result.getOutcome().getKind() != org.starling.contracts.OutcomeKind.OUTCOME_KIND_SUCCESS) {
                continue;
            }

            RoomSnapshot snapshot = result.getSnapshot();
            String payload = buildUserStatusPayload(snapshot);
            String roomKey = roomKey(snapshot.getRoomType(), snapshot.getRoomId());
            String previous = lastStatusByRoom.put(roomKey, payload);
            if (payload.equals(previous)) {
                continue;
            }

            responses.broadcastStatus(
                    GatewayMappings.sessionsInRoom(snapshot.getRoomType(), snapshot.getRoomId()),
                    snapshot
            );
        }
    }

    /**
     * Returns one anchor session per active room.
     * @return the result of this operation
     */
    private Map<String, Session> activeRoomAnchors() {
        Map<String, Session> anchors = new ConcurrentHashMap<>();
        for (Session session : PlayerManager.getInstance().getOnlineSessions()) {
            Session.RoomPresence presence = session.getRoomPresence();
            if (!presence.active()) {
                continue;
            }
            RoomType roomType = presence.type() == Session.RoomType.PUBLIC
                    ? RoomType.ROOM_TYPE_PUBLIC
                    : RoomType.ROOM_TYPE_PRIVATE;
            anchors.putIfAbsent(roomKey(roomType, presence.roomId()), session);
        }
        return anchors;
    }

    /**
     * Builds a compact room key.
     * @param roomType the room type value
     * @param roomId the room id value
     * @return the result of this operation
     */
    private String roomKey(RoomType roomType, int roomId) {
        return roomType.name() + ":" + roomId;
    }

    /**
     * Serializes occupant positions for change detection.
     * @param snapshot the snapshot value
     * @return the result of this operation
     */
    private String buildUserStatusPayload(RoomSnapshot snapshot) {
        StringBuilder payload = new StringBuilder();
        for (RoomOccupant occupant : snapshot.getOccupantsList()) {
            payload.append(occupant.getPlayerId()).append(' ')
                    .append(occupant.getX()).append(',')
                    .append(occupant.getY()).append(',')
                    .append(formatHeight(occupant.getZ())).append(',')
                    .append(occupant.getBodyRotation()).append(',')
                    .append(occupant.getHeadRotation()).append('/');
            if (occupant.getHasNextPosition()) {
                payload.append("mv ")
                        .append(occupant.getNextX()).append(',')
                        .append(occupant.getNextY()).append(',')
                        .append(formatHeight(occupant.getNextZ())).append('/');
            }
            payload.append('\r');
        }
        return payload.toString();
    }

    /**
     * Formats height.
     * @param value the value value
     * @return the result of this operation
     */
    private String formatHeight(double value) {
        return Math.floor(value) == value ? Integer.toString((int) value) : Double.toString(value);
    }

    /**
     * Creates the dedicated daemon thread that syncs room state.
     */
    private static final class RoomSyncThreadFactory implements ThreadFactory {

        /**
         * News thread.
         * @param runnable the runnable value
         * @return the result of this operation
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "starling-gateway-room-sync");
            thread.setDaemon(true);
            return thread;
        }
    }
}
