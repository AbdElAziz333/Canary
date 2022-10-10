package com.abdelaziz.canary.common.entity;

public interface EquipmentEntity {
    default void canaryOnEquipmentChanged() {
    }

    interface EquipmentTrackingEntity {
    }
}
