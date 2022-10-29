package com.abdelaziz.canary.common.block.entity.inventory_change_tracking;

import net.minecraft.inventory.Inventory;

public interface InventoryChangeListener {
    default void handleStackListReplaced(Inventory inventory) {
        this.handleInventoryRemoved(inventory);
    }

    void handleInventoryContentModified(Inventory inventory);

    void handleInventoryRemoved(Inventory inventory);

    void handleComparatorAdded(Inventory inventory);
}
