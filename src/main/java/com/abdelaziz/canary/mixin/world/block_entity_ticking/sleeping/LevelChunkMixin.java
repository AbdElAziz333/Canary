package com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping;

import com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping.WrappedBlockEntityTickInvokerAccessor;
import com.abdelaziz.canary.common.block.entity.SleepingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {

    @Inject(
            method = "method_31719",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addBlockEntityTicker(Lnet/minecraft/world/level/block/entity/TickingBlockEntity;)V" ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void setBlockEntityTickingOrder(BlockEntity blockEntity, BlockEntityTicker<?> blockEntityTicker, BlockPos pos, @Coerce Object wrappedBlockEntityTickInvoker, CallbackInfoReturnable<?> cir, TickingBlockEntity blockEntityTickInvoker, @Coerce Object wrappedBlockEntityTickInvoker2) {
        if (blockEntity instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.setTickWrapper((WrappedBlockEntityTickInvokerAccessor) wrappedBlockEntityTickInvoker2);
        }
    }

    @Inject(
            method = "method_31719",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk$BoundTickingBlockEntity;setWrapped(Lnet/minecraft/world/level/block/entity/TickingBlockEntity;)V" ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void setBlockEntityTickingOrder(BlockEntity blockEntity, BlockEntityTicker<?> blockEntityTicker, BlockPos pos, @Coerce Object wrappedBlockEntityTickInvoker, CallbackInfoReturnable<?> cir, TickingBlockEntity blockEntityTickInvoker) {
        if (blockEntity instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.setTickWrapper((WrappedBlockEntityTickInvokerAccessor) wrappedBlockEntityTickInvoker);
        }
    }

}
