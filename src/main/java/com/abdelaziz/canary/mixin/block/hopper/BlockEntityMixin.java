package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import com.abdelaziz.canary.common.hopper.RemovalCounter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements RemovalCounter {
    //keep track how often the blockentity was removed from the world, e.g. by unloading, breaking or mods
    //the count is used by caches to realize the blockentity might have changed position or otherwise
    private int removedCount;

    @Inject(method = "setRemoved()V", at = @At("HEAD"))
    private void increaseRemovedCount(CallbackInfo ci) {
        this.increaseRemovedCounter();
    }

    @Override
    public int getRemovedCountCanary() {
        return this.removedCount;
    }

    @Override
    public void increaseRemovedCounter() {
        if (this.removedCount != -1) {
            ++this.removedCount;
        }
        if (this instanceof InventoryChangeTracker inventoryChangeTracker) {
            inventoryChangeTracker.emitRemoved();
        }
    }
}