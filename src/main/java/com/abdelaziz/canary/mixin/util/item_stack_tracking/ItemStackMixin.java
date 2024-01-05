package com.abdelaziz.canary.mixin.util.item_stack_tracking;

import com.abdelaziz.canary.common.entity.item.ItemStackSubscriber;
import com.abdelaziz.canary.common.entity.item.ItemStackSubscriberMulti;
import com.abdelaziz.canary.common.hopper.NotifyingItemStack;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements NotifyingItemStack {
    @Shadow
    private int count;

    private int mySlot;

    @Nullable
    private ItemStackSubscriber stackChangeSubscriber;

    @ModifyVariable(method = "setCount(I)V", at = @At("HEAD"), argsOnly = true)
    public int updateInventory(int count) {
        if (stackChangeSubscriber != null && this.count != count) {
            stackChangeSubscriber.notifyBeforeCountChange(mySlot, count);
        }

        return count;
    }

    @Override
    public void canary$subscribe(ItemStackSubscriber subscriber) {
        this.canary$subscribeWithIndex(subscriber, -1);
    }

    @Override
    public void canary$subscribeWithIndex(ItemStackSubscriber subscriber, int mySlot) {
        if (this.stackChangeSubscriber != null) {
            this.canary$registerMultipleSubscribers(subscriber, mySlot);
        } else {
            this.stackChangeSubscriber = subscriber;
            this.mySlot = mySlot;
        }
    }

    @Override
    public void canary$unsubscribe(ItemStackSubscriber stackList) {
        this.canary$unsubscribeWithIndex(stackList, -1);
    }

    @Override
    public void canary$unsubscribeWithIndex(ItemStackSubscriber myInventoryList, int index) {
        if (this.stackChangeSubscriber == myInventoryList) {
            this.stackChangeSubscriber = null;
            this.mySlot = -1;
        } else if (this.stackChangeSubscriber instanceof ItemStackSubscriberMulti multiSubscriber) {
            this.stackChangeSubscriber = multiSubscriber.without(myInventoryList, index);
            this.mySlot = multiSubscriber.getSlot(this.stackChangeSubscriber);
        }  //else: no change, since the inventory wasn't subscribed
    }

    private void canary$registerMultipleSubscribers(ItemStackSubscriber subscriber, int slot) {
        if (this.stackChangeSubscriber instanceof ItemStackSubscriberMulti multiSubscriber) {
            this.stackChangeSubscriber = multiSubscriber.with(subscriber, slot);
        } else {
            this.stackChangeSubscriber = new ItemStackSubscriberMulti(this.stackChangeSubscriber, this.mySlot, subscriber, slot);
            this.mySlot = -1;
        }
    }
}