package com.abdelaziz.canary.mixin.util.chunk_access;

import com.abdelaziz.canary.common.world.ChunkView;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelReader.class)
public interface LevelReaderMixin extends ChunkView {
    @Shadow
    @Nullable
    ChunkAccess getChunk(int i, int i1, ChunkStatus chunkStatus, boolean b);

    @Override
    default @Nullable ChunkAccess getLoadedChunk(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }
}
