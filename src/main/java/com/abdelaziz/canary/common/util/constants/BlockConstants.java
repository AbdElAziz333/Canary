package com.abdelaziz.canary.common.util.constants;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public interface BlockConstants {
    BlockState AIR_BLOCK_STATE = Blocks.AIR.defaultBlockState();
    FluidState EMPTY_FLUID_STATE = Fluids.EMPTY.defaultFluidState();
    //TODO: LayerLightEngine should be overwrited
    //BlockState BEDROCK_BLOCK_STATE = Blocks.BEDROCK.defaultBlockState();
    BlockState VOID_AIR_BLOCK_STATE = Blocks.VOID_AIR.defaultBlockState();
}
