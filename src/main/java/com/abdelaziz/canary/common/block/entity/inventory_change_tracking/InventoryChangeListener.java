package com.abdelaziz.canary.common.block.entity.inventory_change_tracking;

import net.minecraft.world.Container;

public interface InventoryChangeListener {
    default void handleStackListReplaced(Container inventory) {
        this.handleInventoryRemoved(inventory);
    }

    void handleInventoryContentModified(Container inventory);

    void handleInventoryRemoved(Container inventory);

    void handleComparatorAdded(Container inventory);
}
