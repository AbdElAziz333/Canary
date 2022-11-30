package com.abdelaziz.canary.mixin.world.combined_heightmap_update;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;

@Mixin(Heightmap.class)
public interface HeightmapAccessor {
    @Invoker("setHeight")
    void setHeight(int x, int z, int height);

    @Accessor("isOpaque")
    Predicate<BlockState> getOpaque();
}
