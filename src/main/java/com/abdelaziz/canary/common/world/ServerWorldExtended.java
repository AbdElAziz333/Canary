package com.abdelaziz.canary.common.world;

import net.minecraft.world.entity.Mob;

public interface ServerWorldExtended {
    void setNavigationActive(Mob mobEntity);

    void setNavigationInactive(Mob mobEntity);
}
