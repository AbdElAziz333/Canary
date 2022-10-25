package com.abdelaziz.canary.mixin.gen.chunk_region;

import com.abdelaziz.canary.common.util.Pos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements WorldGenLevel {
    @Shadow
    @Final
    private ChunkPos firstPos;

    @Shadow
    @Final
    private int size;

    // Array view of the chunks in the region to avoid an unnecessary de-reference
    private ChunkAccess[] chunksArr;

    // The starting position of this region
    private int minChunkX, minChunkZ;

    /**
     * @author JellySquid
     */
    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Ljava/util/List;Lnet/minecraft/world/level/chunk/ChunkStatus;I)V", at = @At("RETURN"))
    private void init(ServerLevel world, List<ChunkAccess> chunks, ChunkStatus chunkStatus, int i, CallbackInfo ci) {
        this.minChunkX = this.firstPos.x;
        this.minChunkZ = this.firstPos.z;

        this.chunksArr = chunks.toArray(new ChunkAccess[0]);
    }

    /**
     * @reason Avoid pointer de-referencing, make method easier to inline
     * @author JellySquid
     */
    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        int x = (Pos.ChunkCoord.fromBlockCoord(pos.getX())) - this.minChunkX;
        int z = (Pos.ChunkCoord.fromBlockCoord(pos.getZ())) - this.minChunkZ;
        int w = this.size;

        if (x >= 0 && z >= 0 && x < w && z < w) {
            return this.chunksArr[x + z * w].getBlockState(pos);
        } else {
            throw new NullPointerException("No chunk exists at " + new ChunkPos(pos));
        }
    }

    /**
     * @reason Use the chunk array for faster access
     * @author SuperCoder7979, 2No2Name
     */
    @Overwrite
    public ChunkAccess getChunk(int chunkX, int chunkZ) {
        int x = chunkX - this.minChunkX;
        int z = chunkZ - this.minChunkZ;
        int w = this.size;

        if (x >= 0 && z >= 0 && x < w && z < w) {
            return this.chunksArr[x + z * w];
        } else {
            throw new NullPointerException("No chunk exists at " + new ChunkPos(chunkX, chunkZ));
        }
    }

    /**
     * Use our chunk fetch function
     */
    public ChunkAccess getChunk(BlockPos pos) {
        // Skip checking chunk.getStatus().isAtLeast(ChunkStatus.EMPTY) here, because it is always true
        return this.getChunk(Pos.ChunkCoord.fromBlockCoord(pos.getX()), Pos.ChunkCoord.fromBlockCoord(pos.getZ()));
    }
}
