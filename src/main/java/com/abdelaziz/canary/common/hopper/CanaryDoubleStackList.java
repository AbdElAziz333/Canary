package com.abdelaziz.canary.common.hopper;

import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import com.abdelaziz.canary.mixin.block.hopper.DoubleInventoryAccessor;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Class to allow DoubleInventory to have CanaryStackList optimizations.
 * The objects should be immutable and their state should be limited to the first and second inventory.
 * Other state must be managed carefully, as at any time objects of this class may be replaced with new instances.
 */
public class CanaryDoubleStackList extends CanaryStackList {
    private final CanaryStackList first;
    private final CanaryStackList second;

    private long signalStrengthChangeCount;

    public CanaryDoubleStackList(CanaryStackList first, CanaryStackList second, int maxCountPerStack) {
        super(maxCountPerStack);
        this.first = first;
        this.second = second;
    }

    public static CanaryStackList getOrCreate(CanaryStackList first, CanaryStackList second, int maxCountPerStack) {
        CanaryDoubleStackList parentStackList = first.parent;
        if (parentStackList == null || parentStackList != second.parent) {
            parentStackList = new CanaryDoubleStackList(first, second, maxCountPerStack);
            first.parent = parentStackList;
            second.parent = parentStackList;
        }
        return parentStackList;
    }

    @Override
    public long getModCount() {
        return this.first.getModCount() + this.second.getModCount();
    }

    @Override
    public void changedALot() {
        throw new UnsupportedOperationException("Call changed() on the inventory half only!");
    }

    @Override
    public void changed() {
        throw new UnsupportedOperationException("Call changed() on the inventory half only!");
    }

    @Override
    public ItemStack set(int index, ItemStack element) {
        if (index >= this.first.size()) {
            return this.second.set(index - this.first.size(), element);
        } else {
            return this.first.set(index, element);
        }
    }

    @Override
    public void add(int slot, ItemStack element) {
        throw new UnsupportedOperationException("Call add(int value, ItemStack element) on the inventory half only!");
    }

    @Override
    public ItemStack remove(int index) {
        throw new UnsupportedOperationException("Call remove(int value, ItemStack element) on the inventory half only!");
    }

    @Override
    public void clear() {
        this.first.clear();
        this.second.clear();
    }

    @Override
    public int getSignalStrength(Container inventory) {
        //signal strength override state has to be stored in the halves, because this object may be replaced with a copy at any time
        boolean signalStrengthOverride = this.first.hasSignalStrengthOverride() || this.second.hasSignalStrengthOverride();
        if (signalStrengthOverride) {
            return 0;
        }
        int cachedSignalStrength = this.cachedSignalStrength;
        if (cachedSignalStrength == -1 || this.getModCount() != this.signalStrengthChangeCount) {
            cachedSignalStrength = this.calculateSignalStrength(Integer.MAX_VALUE);
            this.signalStrengthChangeCount = this.getModCount();
            this.cachedSignalStrength = cachedSignalStrength;
            return cachedSignalStrength;
        }
        return cachedSignalStrength;
    }

    @Override
    public void setReducedSignalStrengthOverride() {
        this.first.setReducedSignalStrengthOverride();
        this.second.setReducedSignalStrengthOverride();
    }

    @Override
    public void clearSignalStrengthOverride() {
        this.first.clearSignalStrengthOverride();
        this.second.clearSignalStrengthOverride();
    }

    /**
     * @param masterStackList the stacklist of the inventory that comparators read from (double inventory for double chests)
     * @param inventory       the blockentity / inventory that this stacklist is inside
     */
    public void runComparatorUpdatePatternOnFailedExtract(CanaryStackList masterStackList, Container inventory) {
        if (inventory instanceof CompoundContainer) {
            this.first.runComparatorUpdatePatternOnFailedExtract(
                    this, ((DoubleInventoryAccessor) inventory).getFirst()
            );
            this.second.runComparatorUpdatePatternOnFailedExtract(
                    this, ((DoubleInventoryAccessor) inventory).getSecond()
            );
        }
    }

    @NotNull
    @Override
    public ItemStack get(int index) {
        return index >= this.first.size() ? this.second.get(index - this.first.size()) : this.first.get(index);
    }

    @Override
    public int size() {
        return this.first.size() + this.second.size();
    }

    public void setInventoryModificationCallback(InventoryChangeTracker inventoryModificationCallback) {
        this.first.setInventoryModificationCallback(inventoryModificationCallback);
        this.second.setInventoryModificationCallback(inventoryModificationCallback);
    }
}
