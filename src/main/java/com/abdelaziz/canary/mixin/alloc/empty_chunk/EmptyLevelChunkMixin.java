package com.abdelaziz.canary.mixin.alloc.empty_chunk;

import com.abdelaziz.canary.common.util.constants.BlockConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EmptyLevelChunk.class)
public abstract class EmptyLevelChunkMixin {
    /**
     * @reason avoid allocations
     * @author AbdElAziz
     */
    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        return BlockConstants.VOID_AIR_BLOCK_STATE;
    }

    /**
     * @reason avoid allocations
     * @author AbdElAziz
     */
    @Overwrite
    public FluidState getFluidState(BlockPos pos) {
        return BlockConstants.EMPTY_FLUID_STATE;
    }
}