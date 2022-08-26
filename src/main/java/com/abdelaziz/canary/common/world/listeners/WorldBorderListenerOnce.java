package com.abdelaziz.canary.common.world.listeners;

import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

public interface WorldBorderListenerOnce extends BorderChangeListener {

    void onWorldBorderShapeChange(WorldBorder worldBorder);

    default void onAreaReplaced(WorldBorder border) {
        this.onWorldBorderShapeChange(border);
    }

    @Override
    default void onBorderSizeSet(WorldBorder border, double size) {
        this.onWorldBorderShapeChange(border);
    }

    @Override
    default void onBorderSizeLerping(WorldBorder border, double fromSize, double toSize, long time) {
        this.onWorldBorderShapeChange(border);
    }

    @Override
    default void onBorderCenterSet(WorldBorder border, double centerX, double centerZ) {
        this.onWorldBorderShapeChange(border);
    }

    @Override
    default void onBorderSetWarningTime(WorldBorder border, int warningTime) {

    }

    @Override
    default void onBorderSetWarningBlocks(WorldBorder border, int warningBlockDistance) {

    }

    @Override
    default void onBorderSetDamagePerBlock(WorldBorder border, double damagePerBlock) {

    }

    @Override
    default void onBorderSetDamageSafeZOne(WorldBorder border, double safeZoneRadius) {

    }
}
