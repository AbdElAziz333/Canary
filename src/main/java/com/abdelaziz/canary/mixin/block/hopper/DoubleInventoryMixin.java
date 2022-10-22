package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.api.inventory.CanaryInventory;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeEmitter;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import com.abdelaziz.canary.common.block.entity.inventory_comparator_tracking.ComparatorTracker;
import com.abdelaziz.canary.common.hopper.InventoryHelper;
import com.abdelaziz.canary.common.hopper.CanaryDoubleStackList;
import com.abdelaziz.canary.common.hopper.CanaryStackList;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DoubleInventory.class)
public abstract class DoubleInventoryMixin implements CanaryInventory, InventoryChangeTracker, InventoryChangeEmitter, InventoryChangeListener, ComparatorTracker {
    @Shadow
    @Final
    private Inventory first;

    @Shadow
    @Final
    private Inventory second;

    @Shadow
    public abstract int getMaxCountPerStack();

    private CanaryStackList cachedList;

    ReferenceOpenHashSet<InventoryChangeListener> inventoryChangeListeners = null;
    ReferenceOpenHashSet<InventoryChangeListener> inventoryHandlingTypeListeners = null;

    @Override
    public void emitContentModified() {
        ReferenceOpenHashSet<InventoryChangeListener> inventoryChangeListeners = this.inventoryChangeListeners;
        if (inventoryChangeListeners != null) {
            //for (int i = inventoryChangeListeners.size() - 1; i >= 0; i--) {
            //InventoryChangeListener inventoryChangeListener = inventoryChangeListeners.remove(i);
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
        if (this.cachedList != null) {
            return this.cachedList;
        }
        return this.cachedList = CanaryDoubleStackList.getOrCreate(
                InventoryHelper.getCanaryStackList((CanaryInventory) this.first),
                InventoryHelper.getCanaryStackList((CanaryInventory) this.second),
                this.getMaxCountPerStack()
        );
    }

    @Override
    public void setInventoryCanary(DefaultedList<ItemStack> inventory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleStackListReplaced(Inventory inventory) {
        //This inventory object becomes invalid if any of the children stacklists are replaced!
        this.emitRemoved();
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
