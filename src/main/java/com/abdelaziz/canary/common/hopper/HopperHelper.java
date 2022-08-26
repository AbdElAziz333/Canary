package com.abdelaziz.canary.common.hopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class HopperHelper {

    private static final VoxelShape CACHED_INPUT_VOLUME = Hopper.INSIDE;//INPUT_AREA_SHAPE;
    private static final AABB[] CACHED_INPUT_VOLUME_BOXES = CACHED_INPUT_VOLUME.toAabbs().toArray(new AABB[0]);//getBoundingBoxes().toArray(new AABB[0]);

    public static AABB[] getHopperPickupVolumeBoxes(Hopper hopper) {
        VoxelShape inputAreaShape = hopper.getSuckShape();//getInputAreaShape();
        if (inputAreaShape == CACHED_INPUT_VOLUME) {
            return CACHED_INPUT_VOLUME_BOXES;
        }
        return inputAreaShape.toAabbs().toArray(new AABB[0]);//getBoundingBoxes().toArray(new AABB[0]);
    }

    /**
     * Gets the block inventory at the given position, exactly like vanilla gets it.
     * Needed because we don't want to search for entity inventories like the vanilla method does.
     *
     * @param world    world we are searching in
     * @param blockPos position of the block inventory
     * @return the block inventory at the given position
     */
    @Nullable
    public static Container vanillaGetBlockInventory(Level world, BlockPos blockPos) {
        //[VanillaCopy]
        Container inventory = null;
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof WorldlyContainerHolder) {
            inventory = ((WorldlyContainerHolder)block).getContainer(blockState, world, blockPos);
        } else if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof Container) {
                inventory = (Container)blockEntity;
                if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    inventory = ChestBlock.getContainer((ChestBlock)block, blockState, world, blockPos, true);
                }
            }
        }
        return inventory;
    }

    public static boolean tryMoveSingleItem(Container to, ItemStack stack, @Nullable Direction fromDirection) {
        WorldlyContainer toSided = to instanceof WorldlyContainer ? ((WorldlyContainer) to) : null;
        if (toSided != null && fromDirection != null) {
            int[] slots = toSided.getSlotsForFace(fromDirection);//getAvailableSlots(fromDirection);

            for(int slotIndex = 0; slotIndex < slots.length; ++slotIndex) {
                if (tryMoveSingleItem(to, toSided, stack, slots[slotIndex], fromDirection)) {
                    return true; //caller needs to take the item from the original inventory and call to.markDirty()
                }
            }
        } else {
            int j = to.getContainerSize();//size();
            for(int slot = 0; slot < j; ++slot) {
                if (tryMoveSingleItem(to, toSided, stack, slot, fromDirection)) {
                    return true; //caller needs to take the item from the original inventory and call to.markDirty()
                }
            }
        }
        return false;
    }

    public static boolean tryMoveSingleItem(Container to, @Nullable WorldlyContainer toSided, ItemStack transferStack, int targetSlot, @Nullable Direction fromDirection) {
        ItemStack toStack = to.getItem(targetSlot);//getStack(targetSlot);
        //Assumption: no mods depend on the the stack size of transferStack in isValid or canInsert, like vanilla doesn't
        if (to.canPlaceItem(targetSlot, transferStack) && (toSided == null || toSided.canPlaceItemThroughFace(targetSlot, transferStack, fromDirection))) { //isValid - canInsert
            int toCount;
            if (toStack.isEmpty()) {
                ItemStack singleItem = transferStack.split(1);
                to.setItem(targetSlot, singleItem); //setStack
                return true; //caller needs to call to.markDirty() //getMaxCount                                                // getMaxCountPerStack - areNbtEqual
            } else if (toStack.is(transferStack.getItem()) && toStack.getMaxStackSize() > (toCount = toStack.getCount()) && to.getMaxStackSize() > toCount && ItemStack.tagMatches(toStack, transferStack)) {
                transferStack.split(1); //decrement
                toStack.shrink(1); //increment
                return true; //caller needs to call to.markDirty()
            }
        }
        return false;
    }

    private static int calculateReducedSignalStrength(float contentWeight, int inventorySize, int inventoryMaxCountPerStack, int numOccupiedSlots, int itemStackCount, int itemStackMaxCount) {
        //contentWeight adaption can include rounding error for non power of 2 max stack sizes, which do not exist in vanilla anyways
        int maxStackSize = Math.min(inventoryMaxCountPerStack, itemStackMaxCount);
        int newNumOccupiedSlots = numOccupiedSlots - (itemStackCount == 1 ? 1 : 0);
        float newContentWeight = contentWeight - (1f / (float) maxStackSize);
        newContentWeight /= (float) inventorySize;
        return Mth.floor(newContentWeight * 14.0F) + (newNumOccupiedSlots > 0 ? 1 : 0);
    }

    public static ComparatorUpdatePattern determineComparatorUpdatePattern(Container from, LithiumStackList fromStackList) {
        if ((from instanceof HopperBlockEntity) || !(from instanceof RandomizableContainerBlockEntity)) {
            return ComparatorUpdatePattern.NO_UPDATE;
        }
        //calculate the signal strength of the inventory, but also keep the content weight variable
        float contentWeight = 0f;
        int numOccupiedSlots = 0;

        for (int j = 0; j < from.getContainerSize(); ++j) { //size
            ItemStack itemStack = from.getItem(j);//getStack(j);
            if (!itemStack.isEmpty()) { //getMaxCountPerStack() - getMaxCount()
                int maxStackSize = Math.min(from.getMaxStackSize(), itemStack.getMaxStackSize());
                contentWeight += itemStack.getCount() / (float) maxStackSize;
                ++numOccupiedSlots;
            }
        }
        float f = contentWeight;
        f /= (float)from.getContainerSize();
        int originalSignalStrength = Mth.floor(f * 14.0F) + (numOccupiedSlots > 0 ? 1 : 0);


        ComparatorUpdatePattern updatePattern = ComparatorUpdatePattern.NO_UPDATE;
        //check the signal strength change when failing to extract from each slot - getAvailableSlots
        int[] availableSlots = from instanceof WorldlyContainer ? ((WorldlyContainer) from).getSlotsForFace(Direction.DOWN) : null;
        WorldlyContainer sidedInventory = from instanceof WorldlyContainer ? (WorldlyContainer) from : null;
        int fromSize = availableSlots != null ? availableSlots.length : from.getContainerSize();
        for (int i = 0; i < fromSize; i++) {
            int fromSlot = availableSlots != null ? availableSlots[i] : i;
            ItemStack itemStack = fromStackList.get(fromSlot); //canExtract
            if (!itemStack.isEmpty() && (sidedInventory == null || sidedInventory.canTakeItemThroughFace(fromSlot, itemStack, Direction.DOWN))) {
                int newSignalStrength = calculateReducedSignalStrength(contentWeight, from.getContainerSize(), from.getMaxStackSize(), numOccupiedSlots, itemStack.getCount(), itemStack.getMaxStackSize());
                if (newSignalStrength != originalSignalStrength) {                                                  //getMaxCountPerStack - getMaxCount()
                    updatePattern = updatePattern.thenDecrementUpdateIncrementUpdate();
                } else {
                    updatePattern = updatePattern.thenUpdate();
                }
                if (!updatePattern.isChainable()) {
                    break; //if the pattern is indistinguishable from all extensions of the pattern, stop iterating
                }
            }
        }
        return updatePattern;
    }
}
