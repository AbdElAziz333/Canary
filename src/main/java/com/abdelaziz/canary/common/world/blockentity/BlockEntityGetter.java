package com.abdelaziz.canary.common.world.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface BlockEntityGetter {
    BlockEntity getLoadedExistingBlockEntity(BlockPos pos);
}
