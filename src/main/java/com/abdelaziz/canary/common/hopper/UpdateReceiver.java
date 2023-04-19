package com.abdelaziz.canary.common.hopper;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HopperBlock;

public interface UpdateReceiver {
    void invalidateCacheOnNeighborUpdate(boolean fromAbove);
    void invalidateCacheOnNeighborUpdate(Direction fromDirection);
}
