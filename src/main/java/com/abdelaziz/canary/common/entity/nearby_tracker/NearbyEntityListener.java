package com.abdelaziz.canary.common.entity.nearby_tracker;

import com.abdelaziz.canary.common.util.tuples.Range6Int;
import net.minecraft.world.entity.Entity;

public interface NearbyEntityListener {
    Range6Int EMPTY_RANGE = new Range6Int(0, 0, 0, -1, -1, -1);

    Range6Int getChunkRange();

    /**
     * Called by the entity tracker when an entity enters the range of this listener.
     */
    void onEntityEnteredRange(Entity entity);

    /**
     * Called by the entity tracker when an entity leaves the range of this listener or is removed from the world.
     */
    void onEntityLeftRange(Entity entity);

    default Class<? extends Entity> getEntityClass() {
        return Entity.class;
    }
}