package org.starling.game.room.collision;

import org.starling.game.room.geometry.RoomStaticItem;

import java.util.Comparator;
import java.util.List;

public final class RoomItemCollisionDetector implements RoomCollisionDetector {

    private static final Comparator<RoomStaticItem> ITEM_HEIGHT =
            Comparator.comparingDouble(RoomStaticItem::walkingHeight);

    @Override
    public void evaluate(RoomCollisionContext context, RoomCollisionState state) {
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
