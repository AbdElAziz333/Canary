package com.abdelaziz.canary.mixin.world.block_entity_ticking.world_border;

import com.abdelaziz.canary.common.world.listeners.WorldBorderListenerOnce;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk.BoundTickingBlockEntity")
public abstract class DirectBlockEntityTickInvokerMixin implements WorldBorderListenerOnce {

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
                    target = "Lnet/minecraft/world/chunk/WorldChunk;canTickBlockEntity(Lnet/minecraft/util/math/BlockPos;)Z"
            )
    )
    private boolean cachedCanTickBlockEntity(LevelChunk instance, BlockPos pos) {
        if (this.isInsideWorldBorder()) {
            Level world = this.worldChunk.getLevel();
            if (world instanceof ServerLevel serverWorld) {
                return this.worldChunk.getLevelType().isAfter(ChunkHolder.LevelType.TICKING) && serverWorld.isChunkLoaded(ChunkPos.toLong(pos));
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
        return this.worldChunk.getLevel().getWorldBorder().contains(this.getPos());
    }

    private void startWorldBorderCaching() {
        this.worldBorderState = (byte) 1;
        WorldBorder worldBorder = this.worldChunk.getLevel().getWorldBorder();
        worldBorder.addListener(this);
        boolean isStationary = worldBorder.getStage() == WorldBorderStage.STATIONARY;
        if (worldBorder.contains(this.getPos())) {
            if (isStationary || worldBorder.getStage() == WorldBorderStage.GROWING) {
                this.worldBorderState |= (byte) 6;
            }
        } else {
            if (isStationary || worldBorder.getStage() == WorldBorderStage.SHRINKING) {
                this.worldBorderState |= (byte) 2;
            }
        }
    }

    @Override
    public void onWorldBorderShapeChange(WorldBorder worldBorder) {
        this.worldBorderState = (byte) 0;
    }
}
