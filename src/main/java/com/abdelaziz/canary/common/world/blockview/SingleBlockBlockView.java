package com.abdelaziz.canary.common.world.blockview;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public record SingleBlockBlockView(BlockState state, BlockPos blockPos) implements BlockGetter {
    public static SingleBlockBlockView of(BlockState blockState, BlockPos blockPos) {
        return new SingleBlockBlockView(blockState, blockPos.immutable());
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        throw new SingleBlockViewException();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (pos.equals(this.blockPos())) {
            return this.state();
        } else {
            throw new SingleBlockViewException();
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (pos.equals(this.blockPos())) {
            return this.state().getFluidState();
        } else {
            throw new SingleBlockViewException();
        }
    }

    @Override
    public int getHeight() {
        throw new SingleBlockViewException();
    }

    @Override
    public int getMinBuildHeight() {
        throw new SingleBlockViewException();
    }

    public static class SingleBlockViewException extends RuntimeException {

    }
}