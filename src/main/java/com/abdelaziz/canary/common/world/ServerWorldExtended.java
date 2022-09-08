package com.abdelaziz.canary.common.world;

import net.minecraft.entity.mob.MobEntity;

public interface ServerWorldExtended {
    void setNavigationActive(MobEntity mobEntity);

    void setNavigationInactive(MobEntity mobEntity);
}
