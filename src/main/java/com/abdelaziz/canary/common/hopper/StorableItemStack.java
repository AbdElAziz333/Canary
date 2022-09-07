package com.abdelaziz.canary.common.hopper;

public interface StorableItemStack {
    void registerToInventory(CanaryStackList itemStacks, int slot);

    void unregisterFromInventory(CanaryStackList myInventoryList);

    void unregisterFromInventory(CanaryStackList stackList, int index);
}
