package com.abdelaziz.canary.mixin.util.inventory_change_listening;

import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class StackListReplacementTracking {

    @Mixin(AbstractFurnaceBlockEntity.class)
    public abstract static class InventoryChangeTrackingAbstractFurnaceBlockEntity implements InventoryChangeTracker {
        //Handled in LockableConainerBlockEntity
    }

    @Mixin(BrewingStandBlockEntity.class)
    public abstract static class InventoryChangeTrackingBrewingStandBlockEntity implements InventoryChangeTracker {
        //Handled in LockableConainerBlockEntity
    }

    @Mixin(BaseContainerBlockEntity.class)
    public abstract static class StackListReplacementTrackingLockableContainerBlockEntity {
        @Inject(method = "load", at = @At("RETURN"))
        public void readNbtStackListReplacement(CompoundTag nbt, CallbackInfo ci) {
            if (this instanceof InventoryChangeTracker inventoryChangeTracker) {
                inventoryChangeTracker.emitStackListReplaced();
            }
        }
    }

    @Mixin(BarrelBlockEntity.class)
    public abstract static class InventoryChangeTrackingBarrelBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setItems", at = @At("RETURN"))
        public void setInventoryStackListReplacement(NonNullList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }

    @Mixin(ChestBlockEntity.class)
    public abstract static class InventoryChangeTrackingChestBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setItems", at = @At("RETURN"))
        public void setInventoryStackListReplacement(NonNullList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }

    @Mixin(DispenserBlockEntity.class)
    public abstract static class InventoryChangeTrackingDispenserBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setItems", at = @At("RETURN"))
        public void setInventoryStackListReplacement(NonNullList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }

    @Mixin(HopperBlockEntity.class)
    public abstract static class InventoryChangeTrackingHopperBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setItems", at = @At("RETURN"))
        public void setInventoryStackListReplacement(NonNullList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }

    @Mixin(ShulkerBoxBlockEntity.class)
    public abstract static class InventoryChangeTrackingShulkerBoxBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setItems", at = @At("RETURN"))
        public void setInventoryStackListReplacement(NonNullList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }
}
