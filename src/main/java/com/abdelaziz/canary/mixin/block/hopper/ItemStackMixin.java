package com.abdelaziz.canary.mixin.block.hopper;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import com.abdelaziz.canary.common.hopper.CanaryStackList;
import com.abdelaziz.canary.common.hopper.StorableItemStack;
import com.abdelaziz.canary.common.util.tuples.RefIntPair;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Set;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements StorableItemStack {

    @Shadow
    private int count;

    private int mySlot;

    @Nullable
    private Object myLocation;

    @Override
    public void registerToInventory(CanaryStackList itemStacks, int mySlot) {
        if (this.myLocation != null) {
            this.canaryRegisterMultipleInventories(itemStacks, mySlot);
        } else {
            this.myLocation = itemStacks;
            this.mySlot = mySlot;
        }
    }

    @Override
    public void unregisterFromInventory(CanaryStackList stackList) {
        this.unregisterFromInventory(stackList, -1);
    }

    @Override
    public void unregisterFromInventory(CanaryStackList myInventoryList, int index) {
        if (this.myLocation == myInventoryList) {
            this.myLocation = null;
            this.mySlot = -1;
        } else if (this.myLocation instanceof Set<?>) {
            this.canaryUnregisterMultipleInventories(myInventoryList, index);
        } else {
            //Todo does this even happen? This seems to be unexpected behavior
            this.myLocation = null;
        }
    }

    @ModifyVariable(method = "setCount(I)V", at = @At("HEAD"), argsOnly = true)
    public int updateInventory(int count) {
        if (this.myLocation != null && this.count != count) {
            if (this.myLocation instanceof CanaryStackList stackList) {
                stackList.beforeSlotCountChange(this.mySlot, count);
            } else {
                this.canaryUpdateMultipleInventories();
            }
        }
        return count;
    }

    private void canaryRegisterMultipleInventories(CanaryStackList itemStacks, int mySlot) {
        Set<RefIntPair<CanaryStackList>> stackLists;
        if (this.myLocation instanceof Set<?>) {
            //noinspection unchecked
            stackLists = (Set<RefIntPair<CanaryStackList>>) this.myLocation;
        } else {
            stackLists = new ObjectOpenHashSet<>();
            if (this.myLocation != null) {
                RefIntPair<CanaryStackList> pair = new RefIntPair<>((CanaryStackList) this.myLocation, this.mySlot);
                stackLists.add(pair);
                this.myLocation = stackLists;
                this.mySlot = -1;
            }
        }
        RefIntPair<CanaryStackList> pair = new RefIntPair<>(itemStacks, mySlot);
        stackLists.add(pair);
    }

    private void canaryUnregisterMultipleInventories(CanaryStackList itemStacks, int mySlot) {
        //Handle shadow item technology correctly (Item in multiple inventories at once!)
        if (this.myLocation instanceof Set<?> set) {
            //noinspection unchecked
            Set<RefIntPair<CanaryStackList>> stackLists = (Set<RefIntPair<CanaryStackList>>) set;
            if (mySlot >= 0) {
                stackLists.remove(new RefIntPair<>(itemStacks, mySlot));
            } else {
                stackLists.removeIf(stackListSlotPair -> stackListSlotPair.left() == itemStacks);
            }

        }
    }

    private void canaryUpdateMultipleInventories() {
        //Handle shadow item technology correctly (Item in multiple inventories at once!)
        if (this.myLocation instanceof Set<?> set) {
            //noinspection unchecked
            Set<RefIntPair<CanaryStackList>> stackLists = (Set<RefIntPair<CanaryStackList>>) set;
            for (RefIntPair<CanaryStackList> stackListLocationPair : stackLists) {
                stackListLocationPair.left().beforeSlotCountChange(stackListLocationPair.right(), count);
            }

        }
    }
}
