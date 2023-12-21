package com.abdelaziz.canary.common.hopper;

public interface RemovalCounter {
    int getRemovedCountCanary();//usages through CanaryInventory

    default void increaseRemovedCounter() {
    }
}
