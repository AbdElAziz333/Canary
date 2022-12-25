package com.abdelaziz.canary.common.entity.nearby_tracker;

import org.jetbrains.annotations.Nullable;

public interface NearbyEntityListenerProvider {
    @Nullable
    NearbyEntityListenerMulti getListener();

    void addListener(NearbyEntityListener listener);
}