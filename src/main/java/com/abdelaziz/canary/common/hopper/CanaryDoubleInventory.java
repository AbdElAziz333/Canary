package com.abdelaziz.canary.common.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeEmitter;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import com.abdelaziz.canary.common.block.entity.inventory_comparator_tracking.ComparatorTracker;
import com.abdelaziz.canary.mixin.block.hopper.DoubleInventoryAccessor;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;

public class CanaryDoubleInventory extends DoubleInventory implements CanaryInventory, InventoryChangeTracker, InventoryChangeEmitter, InventoryChangeListener, ComparatorTracker {

    private final CanaryInventory first;
    private final CanaryInventory second;

    private CanaryStackList doubleStackList;

    ReferenceOpenHashSet<InventoryChangeListener> inventoryChangeListeners = null;
    ReferenceOpenHashSet<InventoryChangeListener> inventoryHandlingTypeListeners = null;

    private CanaryDoubleInventory(CanaryInventory first, CanaryInventory second) {
        super(first, second);
        this.first = first;
        this.second = second;
    }

    /**
     * This method returns the same CanaryDoubleInventory instance for equal (same children in same order)
     * doubleInventory parameters until {@link #emitRemoved()} is called. After that a new CanaryDoubleInventory object
     * may be in use.
     *
     * @param doubleInventory A double inventory
     * @return The only non-removed CanaryDoubleInventory instance for the double inventory. Null if not compatible
     */
    public static CanaryDoubleInventory getCanaryInventory(DoubleInventory doubleInventory) {
        Inventory vanillaFirst = ((DoubleInventoryAccessor) doubleInventory).getFirst();
        Inventory vanillaSecond = ((DoubleInventoryAccessor) doubleInventory).getSecond();
        if (vanillaFirst != vanillaSecond && vanillaFirst instanceof CanaryInventory first && vanillaSecond instanceof CanaryInventory second) {
            CanaryDoubleInventory newDoubleInventory = new CanaryDoubleInventory(first, second);
            CanaryDoubleStackList doubleStackList = CanaryDoubleStackList.getOrCreate(
                    newDoubleInventory,
                    InventoryHelper.getCanaryStackList(first),
                    InventoryHelper.getCanaryStackList(second),
                    newDoubleInventory.getMaxCountPerStack()
            );
            newDoubleInventory.doubleStackList = doubleStackList;
            return doubleStackList.doubleInventory;
        }
        return null;
    }

    @Override
    public void emitContentModified() {
        ReferenceOpenHashSet<InventoryChangeListener> inventoryChangeListeners = this.inventoryChangeListeners;
        if (inventoryChangeListeners != null) {
            for (InventoryChangeListener inventoryChangeListener : inventoryChangeListeners) {
                inventoryChangeListener.handleInventoryContentModified(this);
            }
            inventoryChangeListeners.clear();
        }
    }

    @Override
    public void emitStackListReplaced() {
        ReferenceOpenHashSet<InventoryChangeListener> listeners = this.inventoryHandlingTypeListeners;
        if (listeners != null && !listeners.isEmpty()) {
            listeners.forEach(inventoryChangeListener -> inventoryChangeListener.handleStackListReplaced(this));
        }
        this.invalidateChangeListening();
    }

    @Override
    public void emitRemoved() {
        ReferenceOpenHashSet<InventoryChangeListener> listeners = this.inventoryHandlingTypeListeners;
        if (listeners != null && !listeners.isEmpty()) {
            listeners.forEach(listener -> listener.handleInventoryRemoved(this));
        }
        this.invalidateChangeListening();
    }

    private void invalidateChangeListening() {
        this.inventoryChangeListeners.clear();

        CanaryStackList canaryStackList = InventoryHelper.getCanaryStackListOrNull(this);
        if (canaryStackList != null) {
            canaryStackList.removeInventoryModificationCallback(this);
        }
    }

    @Override
    public void emitFirstComparatorAdded() {
        ReferenceOpenHashSet<InventoryChangeListener> inventoryChangeListeners = this.inventoryChangeListeners;
        if (inventoryChangeListeners != null) {
            for (InventoryChangeListener inventoryChangeListener : inventoryChangeListeners) {
                inventoryChangeListener.handleComparatorAdded(this);
            }
            inventoryChangeListeners.clear();
        }
    }

    @Override
    public void forwardContentChangeOnce(InventoryChangeListener inventoryChangeListener, CanaryStackList stackList, InventoryChangeTracker thisTracker) {
        if (this.inventoryChangeListeners == null) {
            this.inventoryChangeListeners = new ReferenceOpenHashSet<>(1);
        }
        stackList.setInventoryModificationCallback(thisTracker);
        this.inventoryChangeListeners.add(inventoryChangeListener);

    }

    @Override
    public void forwardMajorInventoryChanges(InventoryChangeListener inventoryChangeListener) {
        if (this.inventoryHandlingTypeListeners == null) {
            this.inventoryHandlingTypeListeners = new ReferenceOpenHashSet<>(1);

            ((InventoryChangeTracker) this.first).listenForMajorInventoryChanges(this);
            ((InventoryChangeTracker) this.second).listenForMajorInventoryChanges(this);
        }
        this.inventoryHandlingTypeListeners.add(inventoryChangeListener);
    }

    @Override
    public void stopForwardingMajorInventoryChanges(InventoryChangeListener inventoryChangeListener) {
        if (this.inventoryHandlingTypeListeners != null) {
            this.inventoryHandlingTypeListeners.remove(inventoryChangeListener);
            if (this.inventoryHandlingTypeListeners.isEmpty()) {
                ((InventoryChangeTracker) this.first).stopListenForMajorInventoryChanges(this);
                ((InventoryChangeTracker) this.second).stopListenForMajorInventoryChanges(this);
            }
        }
    }

    @Override
    public DefaultedList<ItemStack> getInventoryCanary() {
        return this.doubleStackList;
    }

    @Override
    public void setInventoryCanary(DefaultedList<ItemStack> inventory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleInventoryContentModified(Inventory inventory) {
        this.emitContentModified();
    }

    @Override
    public void handleInventoryRemoved(Inventory inventory) {
        this.emitRemoved();
    }

    @Override
    public void handleComparatorAdded(Inventory inventory) {
        this.emitFirstComparatorAdded();
    }

    @Override
    public void onComparatorAdded(Direction direction, int offset) {
        throw new UnsupportedOperationException("Call onComparatorAdded(Direction direction, int offset) on the inventory half only!");
    }

    @Override
    public boolean hasAnyComparatorNearby() {
        return ((ComparatorTracker) this.first).hasAnyComparatorNearby() || ((ComparatorTracker) this.second).hasAnyComparatorNearby();
    }
}