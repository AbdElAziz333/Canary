package com.abdelaziz.canary.mixin.world.block_entity_ticking.world_border;

import com.abdelaziz.canary.common.world.listeners.WorldBorderListenerOnce;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderStatus;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityMixin implements WorldBorderListenerOnce {

    @Shadow
    @Final
    LevelChunk worldChunk;

    @Shadow
    public abstract BlockPos getPos();

    private byte worldBorderState = 0;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunk;canTickBlockEntity(Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean cachedCanTickBlockEntity(LevelChunk instance, BlockPos pos) {
        if (this.isInsideWorldBorder()) {
            Level world = this.worldChunk.getLevel();
            if (world instanceof ServerLevel serverWorld) {
                return this.worldChunk.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING) && serverWorld.areEntitiesLoaded(ChunkPos.asLong(pos));
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean isInsideWorldBorder() {
        if (this.worldBorderState == (byte) 0) {
            this.startWorldBorderCaching();
        }

        int worldBorderState = this.worldBorderState;
        if ((worldBorderState & 3) == 3) {
            return (worldBorderState & 4) != 0;
        }
        return this.worldChunk.getLevel().getWorldBorder().isWithinBounds(this.getPos());
    }

    private void startWorldBorderCaching() {
        this.worldBorderState = (byte) 1;
        WorldBorder worldBorder = this.worldChunk.getLevel().getWorldBorder();
        worldBorder.addListener(this);
        boolean isStationary = worldBorder.getStatus() == BorderStatus.STATIONARY;
        if (worldBorder.isWithinBounds(this.getPos())) {
            if (isStationary || worldBorder.getStatus() == BorderStatus.GROWING) {
                this.worldBorderState |= (byte) 6;
            }
        } else {
            if (isStationary || worldBorder.getStatus() == BorderStatus.SHRINKING) {
                this.worldBorderState |= (byte) 2;
            }
        }
    }

    @Override
    public void onWorldBorderShapeChange(WorldBorder worldBorder) {
        this.worldBorderState = (byte) 0;
    }
}
