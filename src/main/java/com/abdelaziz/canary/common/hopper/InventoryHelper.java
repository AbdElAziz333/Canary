package com.abdelaziz.canary.common.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public class InventoryHelper {
    public static CanaryStackList getCanaryStackList(CanaryInventory inventory) {
        NonNullList<ItemStack> stackList = inventory.getInventoryCanary();
        if (stackList instanceof CanaryStackList lithiumStackList) {
            return lithiumStackList;
        }
        return upgradeToCanaryStackList(inventory);
    }

    private static CanaryStackList upgradeToCanaryStackList(CanaryInventory inventory) {
        //generate loot to avoid any problems with directly accessing the inventory slots
        //the loot that is generated here is not generated earlier than in vanilla, because vanilla generates loot
        //when the hopper checks whether the inventory is empty or full
        inventory.generateLootCanary();
        //get the stack list after generating loot, just in case generating loot creates a new stack list
        NonNullList<ItemStack> stackList = inventory.getInventoryCanary();
        CanaryStackList lithiumStackList = new CanaryStackList(stackList, inventory.getMaxStackSize());
        inventory.setInventoryCanary(lithiumStackList);
        return lithiumStackList;
    }
}