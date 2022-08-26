package com.abdelaziz.canary.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface ChunkRandomSource {
    /**
     * Alternative implementation of {@link Level#getRandomPosInChunk(int, int, int, int)} which does not allocate
     * a new {@link BlockPos}.
     */
    void getRandomPosInChunk(int x, int y, int z, int mask, BlockPos.MutableBlockPos out);
}
