package com.abdelaziz.canary.api.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

/**
 * Provides the ability for mods to allow Canary's hopper optimizations to access their inventories' for item transfers.
 * This exists because Canary's optimized hopper logic will only interact with inventories more efficiently than
 * vanilla if the stack list can be directly accessed and replaced with Canary's custom stack list.
 * It is not required to implement this interface, but doing so will allow the mod's inventories to benefit from
 * Canary's optimizations.
 * <p>
 * This interface should be implemented by your {@link net.minecraft.world.Container} or
 * {@link net.minecraft.world.WorldlyContainer} type to access the stack list.
 * <p>
 * An inventory must not extend {@link net.minecraft.world.level.block.entity.BlockEntity} if it has a supporting block that
 * implements {@link net.minecraft.world.WorldlyContainerHolder}.
 * <p>
 * The hopper interaction behavior of a CanaryInventory should only change if the content of the inventory
 * stack list also changes. For example, an inventory which only accepts an item if it already contains an item of the
 * same type would work fine (changing the acceptance condition only happens when changing the inventory contents here).
 * However, an inventory which accepts an item only if a certain block is near its position will need to signal this
 * change to hoppers by calling {@link CanaryDefaultedList#changedInteractionConditions()}.
 *
 * @author 2No2Name
 */
public interface CanaryInventory extends Container {

    /**
     * Getter for the inventory stack list of this inventory.
     *
     * @return inventory stack list
     */
    NonNullList<ItemStack> getInventoryCanary();

    /**
     * Setter for the inventory stack list of this inventory.
     * Used to replace the stack list with Canary's custom stack list.
     *
     * @param inventory inventory stack list
     */
    void setInventoryCanary(NonNullList<ItemStack> inventory);

    /**
     * Generates the loot like a hopper access would do in vanilla.
     * <p>
     * If a modded inventory has custom loot generation code, it will be required to override this
     * loot generation method. Otherwise its loot may be generated too late.
     */
    default void generateLootCanary() {
        if (this instanceof RandomizableContainerBlockEntity) {
            ((RandomizableContainerBlockEntity) this).unpackLootTable(null);
        }
        if (this instanceof ContainerEntity) {
            ((ContainerEntity) this).unpackChestVehicleLootTable(null);
        }
    }
}
