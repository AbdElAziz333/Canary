package com.abdelaziz.canary.common.hopper;

import com.abdelaziz.canary.common.hopper.CanaryStackList;

public interface StorableItemStack {
    void registerToInventory(CanaryStackList itemStacks, int slot);

    void unregisterFromInventory(CanaryStackList myInventoryList);

    void unregisterFromInventory(CanaryStackList stackList, int index);
}
