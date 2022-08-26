package com.abdelaziz.canary.common.entity;

import net.minecraft.world.entity.ai.navigation.PathNavigation;

public interface NavigatingEntity {
    boolean isRegisteredToWorld();

    void setRegisteredToWorld(PathNavigation navigation);

    PathNavigation getRegisteredNavigation();

    void updateNavigationRegistration();

}
