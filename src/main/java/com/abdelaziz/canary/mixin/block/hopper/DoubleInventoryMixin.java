package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import com.abdelaziz.canary.common.hopper.InventoryHelper;
import com.abdelaziz.canary.common.hopper.CanaryDoubleStackList;
import com.abdelaziz.canary.common.hopper.CanaryStackList;
import com.abdelaziz.canary.common.hopper.RemovalCounter;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DoubleInventory.class)
public abstract class DoubleInventoryMixin implements CanaryInventory, RemovalCounter {
    @Shadow
    @Final
    private Inventory first;

    @Shadow
    @Final
    private Inventory second;
    private CanaryStackList cachedList;

    @Shadow
    public abstract int getMaxCountPerStack();

    @Override
    public int getRemovedCountCanary() {
        return ((CanaryInventory) this.first).getRemovedCountCanary() +
                ((CanaryInventory) this.second).getRemovedCountCanary();
    }

    @Override
    public DefaultedList<ItemStack> getInventoryCanary() {
        if (this.cachedList != null) {
            return this.cachedList;
        }
        return this.cachedList = CanaryDoubleStackList.getOrCreate(
                InventoryHelper.getCanaryStackList((CanaryInventory) this.first),
                InventoryHelper.getCanaryStackList((CanaryInventory) this.second),
                this.getMaxCountPerStack()
        );
    }

    @Override
    public void setInventoryCanary(DefaultedList<ItemStack> inventory) {
        throw new UnsupportedOperationException();
    }
}
