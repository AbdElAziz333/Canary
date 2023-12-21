package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.common.hopper.BlockStateOnlyInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ComposterBlockMixin {
    @Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$InputContainer")
    static abstract class ComposterBlockInputContainerMixin implements BlockStateOnlyInventory {
        @Shadow
        private boolean changed;

        /**
         * Fixes composter inventories becoming blocked forever for no reason, which makes them not cacheable.
         */
        @Inject(
                method = "setChanged()V",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/level/block/ComposterBlock$InputContainer;removeItemNoUpdate(I)Lnet/minecraft/world/item/ItemStack;"
                )
        )
        private void resetDirty(CallbackInfo ci) {
            this.changed = false;
        }
    }

    @Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$EmptyContainer")
    static abstract class ComposterBlockEmptyContainerMixin implements BlockStateOnlyInventory {

    }

    @Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$OutputContainer")
    static abstract class ComposterBlockOutputContainerMixin implements BlockStateOnlyInventory {

    }
}