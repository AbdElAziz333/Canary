package com.abdelaziz.canary.api.inventory;

import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Provides the ability for mods to allow Canary's hopper optimizations to access their inventories' for item transfers.
 * This exists because Canary's optimized hopper logic will only interact with inventories more efficiently than
 * vanilla if the stack list can be directly accessed and replaced with Canary's custom stack list.
 * It is not required to implement this interface, but doing so will allow the mod's inventories to benefit from
 * Canary's optimizations.
 * <p>
 * This interface should be implemented by your {@link net.minecraft.inventory.Inventory} or
 * {@link net.minecraft.inventory.SidedInventory} type to access the stack list.
 * <p>
 * An inventory must not extend {@link net.minecraft.block.entity.BlockEntity} if it has a supporting block that
 * implements {@link net.minecraft.block.InventoryProvider}.
 * <p>
 * The hopper interaction behavior of a CanaryInventory should only change if the content of the inventory
 * stack list also changes. For example, an inventory which only accepts an item if it already contains an item of the
 * same type would work fine (changing the acceptance condition only happens when changing the inventory contents here).
 * However, an inventory which accepts an item only if a certain block is near its position will need to signal this
 * change to hoppers by calling {@link CanaryDefaultedList#changedInteractionConditions()}.
 * If the change cannot be detected with reasonable effort, the inventory will need to call
 * {@link CanaryDefaultedList#setUnstableInteractionConditions()} inside its implementation
 * of setInventoryCanary(DefaultedList) after successfully casting the DefaultedList to CanaryDefaultedList.
 *
 * @author 2No2Name
 */
public interface CanaryInventory extends Inventory {

    /**
     * Getter for the inventory stack list of this inventory.
     *
     * @return inventory stack list
     */
    DefaultedList<ItemStack> getInventoryCanary();

    /**
     * Setter for the inventory stack list of this inventory.
     * Used to replace the stack list with Canary's custom stack list.
     *
     * @param inventory inventory stack list
     */
    void setInventoryCanary(DefaultedList<ItemStack> inventory);

    /**
     * Controls the inventory caching of hoppers.
     * Hoppers update their cache after the removedCounter of the cached inventory increases.
     * <p>
     * Modded inventories most likely do not need to override this method:
     * <p>
     * Entity inventories are never cached, so they do not need to implement this method.
     * Inventories that are created using a block implementing {@link net.minecraft.block.InventoryProvider}
     * must change their BlockState when they become invalid instead.
     * {@link net.minecraft.block.entity.BlockEntity} inventories also do not need to implement this method.
     * Inventories that never become invalid (e.g. by unloading, destroying, being replaced with a new object)
     * do not need to implement this method.
     *
     * @return the removedCounter of the CanaryInventory
     */
    default int getRemovedCountCanary() {
        //Already implemented for BlockEntity and DoubleInventory
        return 0;
    }

    /**
     * Generates the loot like a hopper access would do in vanilla.
     * <p>
     * If a modded inventory has custom loot generation code, it will be required to override this
     * loot generation method. Otherwise its loot may be generated too late.
     */
    default void generateLootCanary() {
        if (this instanceof LootableContainerBlockEntity) {
            ((LootableContainerBlockEntity) this).checkLootInteraction(null);
        }
        if (this instanceof StorageMinecartEntity) {
            ((StorageMinecartEntity) this).generateLoot(null);
        }
    }
}
