package com.abdelaziz.canary.common.util;

import net.minecraft.entity.EquipmentSlot;

/**
 * Pre-initialized constants to avoid unnecessary allocations.
 */
public final class EquipmentSlotConstants {
    public static final EquipmentSlot[] ALL = EquipmentSlot.values();

    private EquipmentSlotConstants() {}
}
