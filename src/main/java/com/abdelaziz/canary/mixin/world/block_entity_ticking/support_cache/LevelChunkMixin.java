package com.abdelaziz.canary.mixin.world.block_entity_ticking.support_cache;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Fix {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#onPlace(Level, BlockPos, BlockState, boolean)}
 * being able to change the blockState but the blockEntity's cached state still being set to the old blockState.
 * This only affects hoppers, as hoppers are the only block with a blockentity that also implements onBlockAdded.
 *
 * @author 2No2name
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Redirect(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/EntityBlock;newBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/entity/BlockEntity;"
            )
    )
    private BlockEntity createBlockEntityWithCachedStateFix(EntityBlock blockEntityProvider, BlockPos pos, BlockState state) {
        return blockEntityProvider.newBlockEntity(pos, this.getBlockState(pos));
    }

    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setBlockState(Lnet/minecraft/world/level/block/state/BlockState;)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void fixCachedState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir, int i, int j, LevelChunkSection chunkSection, boolean bl, int k, int l, int m, BlockState blockState, Block block, BlockEntity blockEntity) {
        BlockState blockState1 = this.getBlockState(pos);
        if (blockState1 != state) {
            //noinspection deprecation
            blockEntity.setBlockState(blockState1);
        }
    }
}
