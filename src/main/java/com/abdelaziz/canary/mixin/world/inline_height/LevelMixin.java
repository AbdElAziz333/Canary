package com.abdelaziz.canary.mixin.world.inline_height;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Implement world height related methods directly instead of going through WorldView and Dimension
 */
@Mixin(Level.class)
public abstract class LevelMixin implements LevelHeightAccessor {
    @Shadow
    public abstract DimensionType dimensionType();

    private int bottomY;
    private int height;
    private int topYInclusive;

    @SuppressWarnings("rawtypes")
    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void initHeightCache(WritableLevelData p_270739_, ResourceKey p_270683_, RegistryAccess p_270200_, Holder p_270240_, Supplier p_270692_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_, CallbackInfo ci) {
        this.height = this.dimensionType().height();
        this.bottomY = this.dimensionType().minY();
        this.topYInclusive = this.bottomY + this.height - 1;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getMinBuildHeight() {
        return this.bottomY;
    }

    @Override
    public int getSectionsCount() {
        return ((this.topYInclusive >> 4) + 1) - (this.bottomY >> 4);
    }

    @Override
    public int getMinSection() {
        return this.bottomY >> 4;
    }

    @Override
    public int getMaxSection() {
        return (this.topYInclusive >> 4) + 1;
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos pos) {
        int y = pos.getY();
        return (y < this.bottomY) || (y > this.topYInclusive);
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return (y < this.bottomY) || (y > this.topYInclusive);
    }

    @Override
    public int getSectionIndex(int y) {
        return (y >> 4) - (this.bottomY >> 4);
    }

    @Override
    public int getSectionIndexFromSectionY(int coord) {
        return coord - (this.bottomY >> 4);

    }

    @Override
    public int getSectionYFromSectionIndex(int index) {
        return index + (this.bottomY >> 4);
    }

    @Override
    public int getMaxBuildHeight() {
        return this.topYInclusive + 1;
    }
}