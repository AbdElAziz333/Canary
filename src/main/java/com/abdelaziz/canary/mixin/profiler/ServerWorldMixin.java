package com.abdelaziz.canary.mixin.profiler;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin extends Level {

    protected ServerWorldMixin(WritableLevelData properties, ResourceKey<Level> registryRef, Holder<DimensionType> dimension, Supplier<ProfilerFiller> supplier, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, dimension, supplier, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Shadow
    @NotNull
    public abstract MinecraftServer getServer();

    @Override
    public ProfilerFiller getProfiler() {
        return this.getServer().getProfiler();
    }
}