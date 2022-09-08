package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

public class InventoryAccessors {
    @Mixin(LootableContainerBlockEntity.class)
    public abstract static class InventoryAccessorLootableContainerBlockEntity implements CanaryInventory {
        @Shadow
        protected abstract DefaultedList<ItemStack> getInvStackList();

        @Shadow
        protected abstract void setInvStackList(DefaultedList<ItemStack> list);

        @Override
        public DefaultedList<ItemStack> getInventoryCanary() {
            return this.getInvStackList();
        }

        @Override
        public void setInventoryCanary(DefaultedList<ItemStack> inventory) {
            this.setInvStackList(inventory);
        }
    }

    @Mixin(AbstractFurnaceBlockEntity.class)
    public abstract static class InventoryAccessorAbstractFurnaceBlockEntity implements CanaryInventory {
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryCanary();

        @Accessor("inventory")
        public abstract void setInventoryCanary(DefaultedList<ItemStack> inventory);
    }

    @Mixin(BrewingStandBlockEntity.class)
    public abstract static class InventoryAccessorBrewingStandBlockEntity implements CanaryInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryCanary(DefaultedList<ItemStack> inventory);
    }

    @Mixin(StorageMinecartEntity.class)
    public abstract static class InventoryAccessorStorageMinecartEntity implements CanaryInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryCanary(DefaultedList<ItemStack> inventory);
    }

}
