package com.abdelaziz.canary.mixin.world.chunk_access;

import com.abdelaziz.canary.common.world.chunk.ChunkHolderExtended;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin implements ChunkHolderExtended {
    @Shadow
    @Final
    private AtomicReferenceArray<CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> futuresByStatus;

    private long lastRequestTime;

    @Override
    public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getFutureByStatus(int index) {
        return this.futuresByStatus.get(index);
    }

    @Override
    public void setFutureForStatus(int index, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future) {
        this.futuresByStatus.set(index, future);
    }

    @Override
    public boolean updateLastAccessTime(long time) {
        long prev = this.lastRequestTime;
        this.lastRequestTime = time;

        return prev != time;
    }
}
