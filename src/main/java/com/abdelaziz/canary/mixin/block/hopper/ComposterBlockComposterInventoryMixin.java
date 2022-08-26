package com.abdelaziz.canary.mixin.block.hopper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$InputContainer")
public abstract class ComposterBlockComposterInventoryMixin { //ComposterInventory
    @Shadow
    private boolean dirty;

    /**
     * Fixes composter inventories becoming blocked forever for no reason, which makes them not cacheable.
     */
    @Inject(
            method = "markDirty()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/ComposterBlock$InputContainer;removeStack(I)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private void resetDirty(CallbackInfo ci) {
        this.dirty = false;
    }
}
