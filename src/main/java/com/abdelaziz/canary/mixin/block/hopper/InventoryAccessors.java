package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import net.minecraft.block.entity.*;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public class InventoryAccessors {
    @Mixin(AbstractFurnaceBlockEntity.class)
    public abstract static class InventoryAccessorAbstractFurnaceBlockEntity implements CanaryInventory {
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryCanary();

        @Accessor("inventory")
        public abstract void setInventoryCanary(DefaultedList<ItemStack> inventory);
    }

    @Mixin(BarrelBlockEntity.class)
    public abstract static class InventoryAccessorBarrelBlockEntity implements CanaryInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryCanary();

        @Override
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

    @Mixin(ChestBlockEntity.class)
    public abstract static class InventoryAccessorChestBlockEntity implements CanaryInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryCanary(DefaultedList<ItemStack> inventory);
    }

    @Mixin(DispenserBlockEntity.class)
    public abstract static class InventoryAccessorDispenserBlockEntity implements CanaryInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryCanary(DefaultedList<ItemStack> inventory);
    }

    @Mixin(HopperBlockEntity.class)
    public abstract static class InventoryAccessorHopperBlockEntity implements CanaryInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryCanary();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryCanary(DefaultedList<ItemStack> inventory);
    }

    @Mixin(ShulkerBoxBlockEntity.class)
    public abstract static class InventoryAccessorShulkerBoxBlockEntity implements CanaryInventory {
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
