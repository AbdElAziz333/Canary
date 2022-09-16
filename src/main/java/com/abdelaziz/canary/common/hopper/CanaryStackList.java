package com.abdelaziz.canary.common.hopper;

import com.abdelaziz.canary.api.inventory.CanaryDefaultedList;
import com.abdelaziz.canary.mixin.block.hopper.DefaultedListAccessor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;

public class CanaryStackList extends DefaultedList<ItemStack> implements CanaryDefaultedList {
    final int maxCountPerStack;

    protected int cachedSignalStrength;
    CanaryDoubleStackList parent; //only used for double chests
    private ComparatorUpdatePattern cachedComparatorUpdatePattern;
    private boolean signalStrengthOverride;
    private long modCount;
    private int occupiedSlots;
    private int fullSlots;

    public CanaryStackList(DefaultedList<ItemStack> original, int maxCountPerStack) {
        //noinspection unchecked
        super(((DefaultedListAccessor<ItemStack>) original).getDelegate(), ItemStack.EMPTY);
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
                if (stack.getMaxCount() <= stack.getCount()) {
                    this.fullSlots++;
                }
                //noinspection ConstantConditions
                ((StorableItemStack) (Object) stack).registerToInventory(this, i);
            }
        }
    }

    public CanaryStackList(int maxCountPerStack) {
        super(null, ItemStack.EMPTY);
        this.maxCountPerStack = maxCountPerStack;
        this.cachedSignalStrength = -1;
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
        for (int i = 0; i < size; i++) {
            ItemStack stack = this.get(i);
            if (!stack.isEmpty()) {
                this.occupiedSlots++;
                if (stack.getMaxCount() <= stack.getCount()) {
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
        int maxCount = stack.getMaxCount();
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
            this.fullSlots += (element.getCount() >= element.getMaxCount() ? 1 : 0) - (previous.getCount() >= previous.getMaxCount() ? 1 : 0);
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

    public int getSignalStrength(Inventory inventory) {
        if (this.signalStrengthOverride) {
            return 0;
        }
        int signalStrength = this.cachedSignalStrength;
        if (signalStrength == -1) {
            return this.cachedSignalStrength = this.calculateSignalStrength(inventory.size());
        }
        return signalStrength;
    }

    /**
     * [VanillaCopy] {@link net.minecraft.screen.ScreenHandler#calculateComparatorOutput(Inventory)}
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
                f += (float) itemStack.getCount() / (float) Math.min(this.maxCountPerStack, itemStack.getMaxCount());
                ++i;
            }
        }

        f /= (float) inventorySize;
        return MathHelper.floor(f * 14.0F) + (i > 0 ? 1 : 0);
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
    public void runComparatorUpdatePatternOnFailedExtract(CanaryStackList masterStackList, Inventory inventory) {
        if (inventory instanceof BlockEntity) {
            if (this.cachedComparatorUpdatePattern == null) {
                this.cachedComparatorUpdatePattern = HopperHelper.determineComparatorUpdatePattern(inventory, masterStackList);
            }
            this.cachedComparatorUpdatePattern.apply((BlockEntity) inventory, masterStackList);
        }
    }

    public int getOccupiedSlots() {
        return this.occupiedSlots;
    }

    public int getFullSlots() {
        return this.fullSlots;
    }

    @Override
    public void changedInteractionConditions() {
        this.modCount++;
    }
}