package org.starling.game.room.collision;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds default and extension collision detectors for live room movement.
 */
public final class RoomCollisionRegistry {

    private static final RoomCollisionRegistry INSTANCE = new RoomCollisionRegistry();

    private final List<RoomCollisionDetector> defaultDetectors = RoomCollisionPipeline.createDefaultDetectors();
    private final CopyOnWriteArrayList<RoomCollisionDetector> prependedDetectors = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<RoomCollisionDetector> appendedDetectors = new CopyOnWriteArrayList<>();

    /**
     * Returns the instance.
     * @return the instance
     */
    public static RoomCollisionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Prepends detector.
     * @param detector the detector value
     */
    public void prependDetector(RoomCollisionDetector detector) {
        if (detector != null) {
            prependedDetectors.add(detector);
        }
    }

    /**
     * Appends detector.
     * @param detector the detector value
     */
    public void appendDetector(RoomCollisionDetector detector) {
        if (detector != null) {
            appendedDetectors.add(detector);
        }
    }

    /**
     * Clears extensions.
     */
    public void clearExtensions() {
        prependedDetectors.clear();
        appendedDetectors.clear();
    }

    /**
     * Snapshots pipeline.
     * @return the resulting snapshot pipeline
     */
    public RoomCollisionPipeline snapshotPipeline() {
        return RoomCollisionPipeline.builder()
                .addDetectors(prependedDetectors)
                .addDetectors(defaultDetectors)
                .addDetectors(appendedDetectors)
                .build();
    }
}
