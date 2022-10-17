package com.abdelaziz.canary.common.entity.pushable;

import net.minecraft.world.level.block.state.BlockState;

public interface BlockCachingEntity {

    default void canaryOnBlockCacheDeleted() {}

    default void canaryOnBlockCacheSet(BlockState newState) {}

    default void canarySetClimbingMobCachingSectionUpdateBehavior(boolean listening) {
        throw new UnsupportedOperationException();
    }

    BlockState getCachedFeetBlockState();
}