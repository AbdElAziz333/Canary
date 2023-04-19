package com.abdelaziz.canary.common.hopper;

import net.minecraft.core.Direction;

public interface UpdateReceiver {
    void invalidateCacheOnNeighborUpdate(boolean fromAbove);
    void invalidateCacheOnNeighborUpdate(Direction fromDirection);
}
