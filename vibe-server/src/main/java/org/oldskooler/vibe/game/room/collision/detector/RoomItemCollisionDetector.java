package org.oldskooler.vibe.game.room.collision.detector;

import org.oldskooler.vibe.game.room.collision.RoomCollisionDetector;
import org.oldskooler.vibe.game.room.geometry.RoomStaticItem;

import java.util.Comparator;
import java.util.List;

/**
 * Applies public-room item walkability and top-height rules to movement steps.
 */
public final class RoomItemCollisionDetector implements RoomCollisionDetector {

    private static final Comparator<RoomStaticItem> ITEM_HEIGHT =
            Comparator.comparingDouble(RoomStaticItem::walkingHeight);

    /**
     * Evaluates.
     * @param context the context value
     * @param state the state value
     */
    @Override
    public void evaluate(Context context, State state) {
        List<RoomStaticItem> items = context.room().getGeometry().itemsAt(context.target());
        if (items.isEmpty()) {
            return;
        }

        RoomStaticItem highestItem = items.stream().max(ITEM_HEIGHT).orElse(null);
        if (highestItem == null) {
            return;
        }

        boolean allowed = context.finalStep()
                ? highestItem.allowsFinalStep()
                : highestItem.allowsIntermediateStep();
        if (!allowed) {
            state.block();
            return;
        }

        state.setWalkHeight(highestItem.walkingHeight());
    }
}
