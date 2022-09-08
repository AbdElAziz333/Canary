package com.abdelaziz.canary.mixin.util.inventory_change_listening;

import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {

    @Inject(
            method = "markRemoved",
            at = @At("RETURN")
    )
    private void updateStackListTracking(CallbackInfo ci) {
        if (this instanceof InventoryChangeTracker inventoryChangeTracker) {
            inventoryChangeTracker.emitRemoved();
        }
    }
}
