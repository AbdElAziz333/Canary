package com.abdelaziz.canary.common.hopper;

import com.abdelaziz.canary.common.entity.item.ItemStackSubscriber;

public interface NotifyingItemStack {
    void canary$subscribe(ItemStackSubscriber subscriber);

    void canary$subscribeWithIndex(ItemStackSubscriber subscriber, int index);

    void canary$unsubscribe(ItemStackSubscriber subscriber);

    void canary$unsubscribeWithIndex(ItemStackSubscriber subscriber, int index);
}