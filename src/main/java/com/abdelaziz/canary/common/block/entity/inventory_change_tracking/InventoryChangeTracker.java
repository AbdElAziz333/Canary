package com.abdelaziz.canary.common.block.entity.inventory_change_tracking;

import com.abdelaziz.canary.common.hopper.LithiumStackList;

public interface InventoryChangeTracker extends InventoryChangeEmitter {
    default void listenForContentChangesOnce(LithiumStackList stackList, InventoryChangeListener inventoryChangeListener) {
        this.forwardContentChangeOnce(inventoryChangeListener, stackList, this);
    }

    default void listenForMajorInventoryChanges(InventoryChangeListener inventoryChangeListener) {
        this.forwardMajorInventoryChanges(inventoryChangeListener);
    }

    default void stopListenForMajorInventoryChanges(InventoryChangeListener inventoryChangeListener) {
        this.stopForwardingMajorInventoryChanges(inventoryChangeListener);
    }
}
