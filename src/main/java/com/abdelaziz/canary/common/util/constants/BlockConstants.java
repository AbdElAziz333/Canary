package com.abdelaziz.canary.common.util.constants;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Pre-initialized constants to avoid unnecessary allocations.
 */
public interface BlockConstants {
    BlockState DEFAULT_BLOCKSTATE = Blocks.AIR.defaultBlockState();
    BlockState VOID_DEFAULT_BLOCKSTATE = Blocks.VOID_AIR.defaultBlockState();
    BlockState OBSIDIAN_BLOCK_STATE = Blocks.OBSIDIAN.defaultBlockState();

    FluidState DEFAULT_FLUIDSTATE = Fluids.EMPTY.defaultFluidState();
}
