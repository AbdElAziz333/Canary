package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.common.hopper.UpdateReceiver;
import com.abdelaziz.canary.common.world.blockentity.BlockEntityGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.renderer.LevelRenderer.DIRECTIONS;

@Mixin(HopperBlock.class)
public abstract class HopperBlockMixin extends BaseEntityBlock {

    protected HopperBlockMixin(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState myBlockState, Direction direction, BlockState newState, LevelAccessor world, BlockPos myPos, BlockPos posFrom) {
        //invalidate cache when composters change state
        if (!world.isClientSide() && newState.getBlock() instanceof WorldlyContainerHolder) {
            this.updateHopper(world, myBlockState, myPos, posFrom);
        }
        return super.updateShape(myBlockState, direction, newState, world, myPos, posFrom);

    }

    @Inject(method = "neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V", at = @At(value = "HEAD"))
    private void updateBlockEntity(BlockState myBlockState, Level world, BlockPos myPos, Block block, BlockPos posFrom, boolean moved, CallbackInfo ci) {
        //invalidate cache when the block is replaced
        if (!world.isClientSide()) {
            this.updateHopper(world, myBlockState, myPos, posFrom);
        }
    }

    private void updateHopper(LevelAccessor world, BlockState myBlockState, BlockPos myPos, BlockPos posFrom) {
        Direction facing = myBlockState.getValue(HopperBlock.FACING);
        boolean above = posFrom.getY() == myPos.getY() + 1;
        if (above || posFrom.getX() == myPos.getX() + facing.getStepX() && posFrom.getY() == myPos.getY() + facing.getStepY() && posFrom.getZ() == myPos.getZ() + facing.getStepZ()) {
            BlockEntity hopper = ((BlockEntityGetter) world).getLoadedExistingBlockEntity(myPos);
            if (hopper instanceof UpdateReceiver updateReceiver) {
                updateReceiver.onNeighborUpdate(above);
            }
        }
    }

    @Inject(method = "onPlace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/HopperBlock;updateEnabled(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", shift = At.Shift.AFTER))
    private void workAroundVanillaUpdateSuppression(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved, CallbackInfo ci) {
        //invalidate caches of nearby hoppers when placing an update suppressed hopper
        if (world.getBlockState(pos) != state) {
            for (Direction direction : DIRECTIONS) {
                BlockEntity hopper = ((BlockEntityGetter) world).getLoadedExistingBlockEntity(pos.relative(direction));
                if (hopper instanceof UpdateReceiver updateReceiver) {
                    updateReceiver.onNeighborUpdate(direction == Direction.DOWN);
                }
            }
        }
    }
}
