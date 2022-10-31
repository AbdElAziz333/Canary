package com.abdelaziz.canary.common.hopper;

import com.abdelaziz.canary.api.inventory.CanaryDefaultedList;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import com.abdelaziz.canary.mixin.block.hopper.NonNullListAccessor;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class CanaryStackList extends NonNullList<ItemStack> implements CanaryDefaultedList {
    final int maxCountPerStack;

    protected int cachedSignalStrength;
    private ComparatorUpdatePattern cachedComparatorUpdatePattern;

    private boolean signalStrengthOverride;

    private long modCount;
    private int occupiedSlots;
    private int fullSlots;

    CanaryDoubleStackList parent; //only used for double chests

    InventoryChangeTracker inventoryModificationCallback;

    public CanaryStackList(NonNullList<ItemStack> original, int maxCountPerStack) {
        //noinspection unchecked
        super(((NonNullListAccessor<ItemStack>) original).getList(), ItemStack.EMPTY);
        this.maxCountPerStack = maxCountPerStack;

        this.cachedSignalStrength = -1;
        this.cachedComparatorUpdatePattern = null;
        this.modCount = 0;
        this.signalStrengthOverride = false;

        this.occupiedSlots = 0;
        this.fullSlots = 0;
        int size = this.size();
        for (int i = 0; i < size; i++) {
            ItemStack stack = this.get(i);
            if (!stack.isEmpty()) {
                this.occupiedSlots++;
                if (stack.getMaxStackSize() <= stack.getCount()) {
                    this.fullSlots++;
                }
                //noinspection ConstantConditions
                ((StorableItemStack) (Object) stack).registerToInventory(this, i);
            }
        }

        this.inventoryModificationCallback = null;
    }

    public CanaryStackList(int maxCountPerStack) {
        super(null, ItemStack.EMPTY);
        this.maxCountPerStack = maxCountPerStack;
        this.cachedSignalStrength = -1;
        this.inventoryModificationCallback = null;
    }

    public long getModCount() {
        return this.modCount;
    }

    public void changedALot() {
        this.changed();

        //fix the slot mapping of all stacks in the inventory
        //fix occupied/full slot counters
        this.occupiedSlots = 0;
        this.fullSlots = 0;
        int size = this.size();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < size; i++) {
            ItemStack stack = this.get(i);
            if (!stack.isEmpty()) {
                this.occupiedSlots++;
                if (stack.getMaxStackSize() <= stack.getCount()) {
                    this.fullSlots++;
                }
                //noinspection ConstantConditions
                ((StorableItemStack) (Object) stack).unregisterFromInventory(this);
            }
        }
        for (int i = 0; i < size; i++) {
            ItemStack stack = this.get(i);
            if (!stack.isEmpty()) {
                //noinspection ConstantConditions
                ((StorableItemStack) (Object) stack).registerToInventory(this, i);
            }
        }

    }

    public void beforeSlotCountChange(int slot, int newCount) {
        ItemStack stack = this.get(slot);
        int count = stack.getCount();
        if (newCount <= 0) {
            //noinspection ConstantConditions
            ((StorableItemStack) (Object) stack).unregisterFromInventory(this, slot);
        }
        int maxCount = stack.getMaxStackSize();
        this.occupiedSlots -= newCount <= 0 ? 1 : 0;
        this.fullSlots += (newCount >= maxCount ? 1 : 0) - (count >= maxCount ? 1 : 0);

        this.changed();
    }

    /**
     * Method that must be invoked before or after a change of the inventory to update important values. If done too
     * early or too late, behavior might be incorrect.
     */
    public void changed() {
        this.cachedSignalStrength = -1;
        this.cachedComparatorUpdatePattern = null;
        this.modCount++;

        InventoryChangeTracker inventoryModificationCallback = this.inventoryModificationCallback;
        if (inventoryModificationCallback != null) {
            this.inventoryModificationCallback = null;
            inventoryModificationCallback.emitContentModified();
        }
    }

    @Override
    public ItemStack set(int index, ItemStack element) {
        ItemStack previous = super.set(index, element);
        if (previous != element) {
            //noinspection ConstantConditions
            ((StorableItemStack) (Object) previous).unregisterFromInventory(this, index);
            if (!element.isEmpty()) {
                //noinspection ConstantConditions
                ((StorableItemStack) (Object) element).registerToInventory(this, index);
            }

            this.occupiedSlots += (previous.isEmpty() ? 1 : 0) - (element.isEmpty() ? 1 : 0);
            this.fullSlots += (element.getCount() >= element.getMaxStackSize() ? 1 : 0) - (previous.getCount() >= previous.getMaxStackSize() ? 1 : 0);
            this.changed();
        }

        return previous;
    }

    @Override
    public void add(int slot, ItemStack element) {
        super.add(slot, element);
        if (!element.isEmpty()) {
            //noinspection ConstantConditions
            ((StorableItemStack) (Object) element).registerToInventory(this, this.indexOf(element));
        }
        this.changedALot();
    }

    @Override
    public ItemStack remove(int index) {
        ItemStack previous = super.remove(index);
        //noinspection ConstantConditions
        ((StorableItemStack) (Object) previous).unregisterFromInventory(this, index);
        this.changedALot();
        return previous;
    }

    @Override
    public void clear() {
        int size = this.size();
        for (int i = 0; i < size; i++) {
            ItemStack stack = this.get(i);
            if (!stack.isEmpty()) {
                //noinspection ConstantConditions
                ((StorableItemStack) (Object) stack).unregisterFromInventory(this, i);
            }
        }
        super.clear();
        this.changedALot();
    }

    public boolean hasSignalStrengthOverride() {
        return this.signalStrengthOverride;
    }

    public int getSignalStrength(Container inventory) {
        if (this.signalStrengthOverride) {
            return 0;
        }
        int signalStrength = this.cachedSignalStrength;
        if (signalStrength == -1) {
            return this.cachedSignalStrength = this.calculateSignalStrength(inventory.getContainerSize());
        }
        return signalStrength;
    }

    /**
     * [VanillaCopy] {@link net.minecraft.world.inventory.AbstractContainerMenu#getRedstoneSignalFromContainer(Container)}
     *
     * @return the signal strength for this inventory
     */
    int calculateSignalStrength(int inventorySize) {
        int i = 0;
        float f = 0.0F;

        inventorySize = Math.min(inventorySize, this.size());
        for (int j = 0; j < inventorySize; ++j) {
            ItemStack itemStack = this.get(j);
            if (!itemStack.isEmpty()) {
                f += (float) itemStack.getCount() / (float) Math.min(this.maxCountPerStack, itemStack.getMaxStackSize());
                ++i;
            }
        }

        f /= (float) inventorySize;
        return Mth.floor(f * 14.0F) + (i > 0 ? 1 : 0);
    }

    public void setReducedSignalStrengthOverride() {
        this.signalStrengthOverride = true;
    }

    public void clearSignalStrengthOverride() {
        this.signalStrengthOverride = false;
    }

    /**
     * @param masterStackList the stacklist of the inventory that comparators read from (double inventory for double chests)
     * @param inventory       the blockentity / inventory that this stacklist is inside
     */
    public void runComparatorUpdatePatternOnFailedExtract(CanaryStackList masterStackList, Container inventory) {
        if (inventory instanceof BlockEntity) {
            if (this.cachedComparatorUpdatePattern == null) {
                this.cachedComparatorUpdatePattern = HopperHelper.determineComparatorUpdatePattern(inventory, masterStackList);
            }
            this.cachedComparatorUpdatePattern.apply((BlockEntity) inventory, masterStackList);
        }
    }

    public boolean maybeSendsComparatorUpdatesOnFailedExtract() {
        return this.cachedComparatorUpdatePattern == null || this.cachedComparatorUpdatePattern != ComparatorUpdatePattern.NO_UPDATE;
    }

    public int getOccupiedSlots() {
        return this.occupiedSlots;
    }

    public int getFullSlots() {
        return this.fullSlots;
    }

    @Override
    public void changedInteractionConditions() {
        this.changed();
    }


    public void setInventoryModificationCallback(@NotNull InventoryChangeTracker inventoryModificationCallback) {
        if (this.inventoryModificationCallback != null && this.inventoryModificationCallback != inventoryModificationCallback) {
            this.inventoryModificationCallback.emitCallbackReplaced();
        }
        this.inventoryModificationCallback = inventoryModificationCallback;
    }

    public void removeInventoryModificationCallback(@NotNull InventoryChangeTracker inventoryModificationCallback) {
        if (this.inventoryModificationCallback != null && this.inventoryModificationCallback == inventoryModificationCallback) {
            this.inventoryModificationCallback = null;
        }
    }
}