package com.abdelaziz.canary.common.util.constants;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Pre-initialized constants to avoid unnecessary allocations.
 */
public interface BlockConstants {
    BlockState AIR_BLOCK_STATE = Blocks.AIR.defaultBlockState();
    FluidState EMPTY_FLUID_STATE = Fluids.EMPTY.defaultFluidState();
    BlockState VOID_AIR_BLOCK_STATE = Blocks.VOID_AIR.defaultBlockState();
}
