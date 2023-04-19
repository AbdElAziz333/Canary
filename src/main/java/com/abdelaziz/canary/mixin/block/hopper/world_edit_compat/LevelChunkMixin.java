package com.abdelaziz.canary.mixin.block.hopper.world_edit_compat;

import com.abdelaziz.canary.common.compat.WorldEditCompat;
import com.abdelaziz.canary.common.hopper.UpdateReceiver;
import com.abdelaziz.canary.common.util.DirectionConstants;
import com.abdelaziz.canary.common.world.WorldHelper;
import com.abdelaziz.canary.common.world.blockentity.BlockEntityGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow
    public abstract Level getLevel();

    @Inject(
            method = "setBlockState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;onPlace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void updateHoppersIfWorldEditPresent(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        if (WorldEditCompat.WORLD_EDIT_PRESENT && (state.getBlock() instanceof WorldlyContainerHolder || state.hasBlockEntity())) {
            updateHopperCachesOnNewInventoryAdded((LevelChunk) (Object) this, pos, this.getLevel());
        }
    }

    private static void updateHopperCachesOnNewInventoryAdded(LevelChunk worldChunk, BlockPos pos, Level world) {
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        for (Direction offsetDirection : DirectionConstants.ALL) {
            neighborPos.setWithOffset(pos, offsetDirection);
            BlockEntity neighborBlockEntity =
                    WorldHelper.arePosWithinSameChunk(pos, neighborPos) ?
                            worldChunk.getBlockEntity(neighborPos, LevelChunk.EntityCreationType.CHECK) :
                            ((BlockEntityGetter) world).getLoadedExistingBlockEntity(neighborPos);
            if (neighborBlockEntity instanceof UpdateReceiver updateReceiver) {
                updateReceiver.invalidateCacheOnNeighborUpdate(offsetDirection.getOpposite());
            }
        }
    }
}
