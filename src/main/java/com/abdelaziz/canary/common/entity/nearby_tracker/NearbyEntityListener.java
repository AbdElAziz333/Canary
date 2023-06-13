package com.abdelaziz.canary.common.entity.nearby_tracker;

import net.minecraft.world.entity.Entity;

public interface NearbyEntityListener {
    default Class<? extends Entity> getEntityClass() {
        return Entity.class;
    }
}
