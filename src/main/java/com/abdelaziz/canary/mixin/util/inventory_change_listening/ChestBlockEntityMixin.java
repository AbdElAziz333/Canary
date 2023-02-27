package com.abdelaziz.canary.mixin.util.inventory_change_listening;

import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin extends RandomizableContainerBlockEntity implements InventoryChangeEmitter {
    protected ChestBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @SuppressWarnings("deprecation")
    @Intrinsic
    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
    }

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference"})
    @Inject(method = "setBlockState(Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("RETURN"))
    private void emitRemovedOnSetCachedState(CallbackInfo ci) {
        //Handle switching double / single chest state
        this.emitRemoved();
    }
}
