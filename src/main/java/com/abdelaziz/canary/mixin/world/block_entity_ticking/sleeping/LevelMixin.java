package com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public class LevelMixin {

    @Redirect(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;shouldTickBlocksAt(J)Z")
    )
    private boolean shouldTickBlockPosFilterNull(Level instance, long pos) {
        if (pos == Long.MIN_VALUE) {
            return false;
        }
        return instance.shouldTickBlocksAt(pos);
    }

    @Redirect(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;asLong(Lnet/minecraft/core/BlockPos;)J")
    )
    private long shouldTickBlockPosFilterNull(BlockPos pos) {
        if (pos == null) {
            return Long.MIN_VALUE;
        }
        return ChunkPos.asLong(pos);
    }
}
