package com.abdelaziz.canary.common.block.entity.inventory_comparator_tracking;

import net.minecraft.util.math.Direction;

public interface ComparatorTracker {
    void onComparatorAdded(Direction direction, int offset);

    boolean hasAnyComparatorNearby();
}
