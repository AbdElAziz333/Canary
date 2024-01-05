package com.abdelaziz.canary.common.entity.item;

public interface ItemStackSubscriber {
    void notifyBeforeCountChange(int slot, int newCount);
}
