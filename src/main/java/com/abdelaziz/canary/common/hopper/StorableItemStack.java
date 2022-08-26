package com.abdelaziz.canary.common.hopper;

import com.abdelaziz.canary.common.hopper.LithiumStackList;

public interface StorableItemStack {
    void registerToInventory(LithiumStackList itemStacks, int slot);

    void unregisterFromInventory(LithiumStackList myInventoryList);

    void unregisterFromInventory(LithiumStackList stackList, int index);
}
