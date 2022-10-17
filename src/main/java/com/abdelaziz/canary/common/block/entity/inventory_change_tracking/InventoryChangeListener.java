package com.abdelaziz.canary.common.block.entity.inventory_change_tracking;

import net.minecraft.world.Container;

public interface InventoryChangeListener {
    void handleStackListReplaced(Container inventory);

    void handleInventoryContentModified(Container inventory);

    void handleInventoryRemoved(Container inventory);

    void handleComparatorAdded(Container inventory);
}
