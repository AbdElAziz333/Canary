package com.abdelaziz.canary.mixin.util.chunk_access;

import com.abdelaziz.canary.common.world.ChunkView;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PathNavigationRegion.class)
public abstract class PathNavigationRegionMixin implements ChunkView {

    @Shadow
    protected abstract ChunkAccess getChunk(int pX, int pZ);

    @Override
    public @Nullable ChunkAccess getLoadedChunk(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ);
    }
}
