package com.abdelaziz.canary.mixin.util.inventory_change_listening;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeEmitter;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import com.abdelaziz.canary.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import com.abdelaziz.canary.common.hopper.CanaryStackList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;

@Mixin(ContainerEntity.class)
public abstract class ContainerEntityMixin implements InventoryChangeEmitter, Container {
    ArrayList<InventoryChangeListener> inventoryChangeListeners = null;
    ReferenceOpenHashSet<InventoryChangeListener> inventoryHandlingTypeListeners = null;

    @Override
    public void emitContentModified() {
        ArrayList<InventoryChangeListener> inventoryChangeListeners = this.inventoryChangeListeners;
        if (inventoryChangeListeners != null) {
            for (int i = inventoryChangeListeners.size() - 1; i >= 0; i--) {
                InventoryChangeListener inventoryChangeListener = inventoryChangeListeners.remove(i);
                inventoryChangeListener.handleInventoryContentModified(this);
            }
        }
    }

    @Override
    public void emitStackListReplaced() {
        ReferenceOpenHashSet<InventoryChangeListener> listeners = this.inventoryHandlingTypeListeners;
        if (listeners != null && !listeners.isEmpty()) {
            listeners.forEach(inventoryChangeListener -> inventoryChangeListener.handleStackListReplaced(this));
        }

        if (this instanceof InventoryChangeListener listener) {
            listener.handleStackListReplaced(this);
        }
    }

    @Override
    public void emitRemoved() {
        ReferenceOpenHashSet<InventoryChangeListener> listeners = this.inventoryHandlingTypeListeners;
        if (listeners != null && !listeners.isEmpty()) {
            listeners.forEach(listener -> listener.handleInventoryRemoved(this));
        }

        if (this instanceof InventoryChangeListener listener) {
            listener.handleInventoryRemoved(this);
        }
    }

    @Override
    public void emitFirstComparatorAdded() {
        ArrayList<InventoryChangeListener> inventoryChangeListeners = this.inventoryChangeListeners;
        if (inventoryChangeListeners != null) {
            for (int i = inventoryChangeListeners.size() - 1; i >= 0; i--) {
                InventoryChangeListener inventoryChangeListener = inventoryChangeListeners.remove(i);
                inventoryChangeListener.handleComparatorAdded(this);
            }
        }
    }

    @Override
    public void forwardContentChangeOnce(InventoryChangeListener inventoryChangeListener, CanaryStackList stackList, InventoryChangeTracker thisTracker) {
        if (this.inventoryChangeListeners == null) {
            this.inventoryChangeListeners = new ArrayList<>(1);
        }
        stackList.setInventoryModificationCallback(thisTracker);
        this.inventoryChangeListeners.add(inventoryChangeListener);

    }

    @Override
    public void forwardMajorInventoryChanges(InventoryChangeListener inventoryChangeListener) {
        if (this.inventoryHandlingTypeListeners == null) {
            this.inventoryHandlingTypeListeners = new ReferenceOpenHashSet<>(1);
        }
        this.inventoryHandlingTypeListeners.add(inventoryChangeListener);
    }

    @Override
    public void stopForwardingMajorInventoryChanges(InventoryChangeListener inventoryChangeListener) {
        if (this.inventoryHandlingTypeListeners != null) {
            this.inventoryHandlingTypeListeners.remove(inventoryChangeListener);
        }
    }
}
