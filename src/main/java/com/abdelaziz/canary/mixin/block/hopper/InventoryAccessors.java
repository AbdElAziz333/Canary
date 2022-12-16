package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

public class InventoryAccessors {
    @Mixin(AbstractFurnaceBlockEntity.class)
    public abstract static class InventoryAccessorAbstractFurnaceBlockEntity implements CanaryInventory {
        @Accessor("items")
        public abstract NonNullList<ItemStack> getInventoryCanary();

        @Accessor("items")
        public abstract void setInventoryCanary(NonNullList<ItemStack> inventory);
    }

    @Mixin(BarrelBlockEntity.class)
    public abstract static class InventoryAccessorBarrelBlockEntity implements CanaryInventory {
        @Accessor("items")
        public abstract NonNullList<ItemStack> getInventoryCanary();

        @Accessor("items")
        public abstract void setInventoryCanary(NonNullList<ItemStack> inventory);
    }

    @Mixin(BrewingStandBlockEntity.class)
    public abstract static class InventoryAccessorBrewingStandBlockEntity implements CanaryInventory {
        @Override
        @Accessor("items")
        public abstract NonNullList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("items")
        public abstract void setInventoryCanary(NonNullList<ItemStack> inventory);
    }

    @Mixin(ChestBlockEntity.class)
    public abstract static class InventoryAccessorChestBlockEntity implements CanaryInventory {
        @Override
        @Accessor("items")
        public abstract NonNullList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("items")
        public abstract void setInventoryCanary(NonNullList<ItemStack> inventory);
    }

    @Mixin(DispenserBlockEntity.class)
    public abstract static class InventoryAccessorDispenserBlockEntity implements CanaryInventory {
        @Override
        @Accessor("items")
        public abstract NonNullList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("items")
        public abstract void setInventoryCanary(NonNullList<ItemStack> inventoryCanary);
    }

    @Mixin(HopperBlockEntity.class)
    public abstract static class InventoryAccessorHopperBlockEntity implements CanaryInventory {
        @Override
        @Accessor("items")
        public abstract NonNullList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("items")
        public abstract void setInventoryCanary(NonNullList<ItemStack> inventoryCanary);
    }

    @Mixin(ShulkerBoxBlockEntity.class)
    public abstract static class InventoryAccessorShulkerBoxBlockEntity implements CanaryInventory {
        @Override
        @Accessor("itemStacks")
        public abstract NonNullList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("itemStacks")
        public abstract void setInventoryCanary(NonNullList<ItemStack> inventoryCanary);
    }

    @Mixin(RandomizableContainerBlockEntity.class)
    public abstract static class InventoryAccessorRandomizableContainerBlockEntity implements CanaryInventory {
        @Shadow
        protected abstract NonNullList<ItemStack> getItems();

        @Shadow
        protected abstract void setItems(NonNullList<ItemStack> list);

        @Override
        public NonNullList<ItemStack> getInventoryCanary() {
            return this.getItems();
        }

        @Override
        public void setInventoryCanary(NonNullList<ItemStack> inventory) {
            this.setItems(inventory);
        }
    }

    @Mixin(AbstractMinecartContainer.class)
    public abstract static class InventoryAccessorAbstractMinecartEntity implements CanaryInventory {
        @Override
        @Accessor("itemStacks")
        public abstract NonNullList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("itemStacks")
        public abstract void setInventoryCanary(NonNullList<ItemStack> inventory);
    }
}
