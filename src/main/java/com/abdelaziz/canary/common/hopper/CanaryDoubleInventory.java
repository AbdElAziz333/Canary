package com.abdelaziz.canary.common.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeEmitter;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import com.abdelaziz.canary.common.block.entity.inventory_comparator_tracking.ComparatorTracker;
import com.abdelaziz.canary.mixin.block.hopper.CompoundContainerAccessor;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class CanaryDoubleInventory extends CompoundContainer implements CanaryInventory, InventoryChangeTracker, InventoryChangeEmitter, InventoryChangeListener, ComparatorTracker {

    private Container container1;

    private Container container2;

    private CanaryStackList doubleStackList;

    ReferenceOpenHashSet<InventoryChangeListener> inventoryChangeListeners = null;
    ReferenceOpenHashSet<InventoryChangeListener> inventoryHandlingTypeListeners = null;

    private CanaryDoubleInventory(CanaryInventory container1, CanaryInventory container2) {
        super(container1, container2);
        this.container1 = container1;
        this.container2 = container2;
    }

    /**
     * This method returns the same CanaryDoubleInventory instance for equal (same children in same order)
     * doubleInventory parameters until {@link #emitRemoved()} is called. After that a new CanaryDoubleInventory object
     * may be in use.
     *
     * @param doubleInventory A double inventory
     * @return The only non-removed CanaryDoubleInventory instance for the double inventory. Null if not compatible
     */
    public static CanaryDoubleInventory getCanaryInventory(CompoundContainer doubleInventory) {
        Container vanillaFirst = ((CompoundContainerAccessor) doubleInventory).getFirst();
        Container vanillaSecond = ((CompoundContainerAccessor) doubleInventory).getSecond();
        if (vanillaFirst != vanillaSecond && vanillaFirst instanceof CanaryInventory container1 && vanillaSecond instanceof CanaryInventory container2) {
            CanaryDoubleInventory newDoubleInventory = new CanaryDoubleInventory(container1, container2);
            CanaryDoubleStackList doubleStackList = CanaryDoubleStackList.getOrCreate(
                    newDoubleInventory,
                    InventoryHelper.getCanaryStackList(container1),
                    InventoryHelper.getCanaryStackList(container2),
                    newDoubleInventory.getMaxStackSize()
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
        if (this.inventoryChangeListeners != null) {
            this.inventoryChangeListeners.clear();
        }

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

            ((InventoryChangeTracker) this.container1).listenForMajorInventoryChanges(this);
            ((InventoryChangeTracker) this.container2).listenForMajorInventoryChanges(this);
        }
        this.inventoryHandlingTypeListeners.add(inventoryChangeListener);
    }

    @Override
    public void stopForwardingMajorInventoryChanges(InventoryChangeListener inventoryChangeListener) {
        if (this.inventoryHandlingTypeListeners != null) {
            this.inventoryHandlingTypeListeners.remove(inventoryChangeListener);
            if (this.inventoryHandlingTypeListeners.isEmpty()) {
                ((InventoryChangeTracker) this.container1).stopListenForMajorInventoryChanges(this);
                ((InventoryChangeTracker) this.container2).stopListenForMajorInventoryChanges(this);
            }
        }
    }

    @Override
    public NonNullList<ItemStack> getInventoryCanary() {
        return this.doubleStackList;
    }

    @Override
    public void setInventoryCanary(NonNullList<ItemStack> inventory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleStackListReplaced(Container inventory) {
        //This inventory object becomes invalid if any of the children stacklists are replaced!
        this.emitRemoved();
    }

    @Override
    public void handleInventoryContentModified(Container inventory) {
        this.emitContentModified();
    }

    @Override
    public void handleInventoryRemoved(Container inventory) {
        this.emitRemoved();
    }

    @Override
    public void handleComparatorAdded(Container inventory) {
        this.emitFirstComparatorAdded();
    }

    @Override
    public void onComparatorAdded(Direction direction, int offset) {
        throw new UnsupportedOperationException("Call onComparatorAdded(Direction direction, int offset) on the inventory half only!");
    }

    @Override
    public boolean hasAnyComparatorNearby() {
        return ((ComparatorTracker) this.container1).hasAnyComparatorNearby() || ((ComparatorTracker) this.container2).hasAnyComparatorNearby();
    }
}
