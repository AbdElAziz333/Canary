package com.abdelaziz.canary.common.entity.nearby_tracker;

import com.abdelaziz.canary.common.util.tuples.Range6Int;
import com.abdelaziz.canary.mixin.util.accessors.PersistentEntitySectionManagerAccessor;
import com.abdelaziz.canary.mixin.util.accessors.ServerLevelAccessor;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows for multiple listeners on an entity to be grouped under one logical listener. No guarantees are made about the
 * order of which each sub-listener will be notified.
 */
public class NearbyEntityListenerMulti implements NearbyEntityListener {
    private final List<NearbyEntityListener> listeners = new ArrayList<>(4);
    private Range6Int range = null;

    public <T extends LivingEntity> void addListener(NearbyEntityTracker<T> tracker) {
        this.listeners.add(tracker);
        this.updateRange(tracker);
    }

    public <T extends LivingEntity> void removeListener(NearbyEntityTracker<T> tracker) {
        this.listeners.remove(tracker);
        this.updateRange(tracker);
    }

    private <S extends EntityAccess, T extends LivingEntity> void updateRange(NearbyEntityTracker<T> tracker) {
        if (this.range == null) {
            return;
        }

        Range6Int updatedRange = this.calculateRange();
        if (!this.range.equals(updatedRange)) {
            this.range = updatedRange;

            //noinspection unchecked
            EntitySectionStorage<S> entityCache = ((PersistentEntitySectionManagerAccessor<S>)((ServerLevelAccessor) tracker.getEntity().getLevel()).getEntityManager()).getSectionStorage();
            SectionPos chunkPos = SectionPos.of(tracker.getEntity().blockPosition());
            this.updateChunkRegistrations(entityCache, chunkPos, this.range, chunkPos, updatedRange);
        }
    }

    @Override
    public Range6Int getChunkRange() {
        if (this.range != null) {
            return this.range;
        }

        return this.calculateRange();
    }

    private Range6Int calculateRange() {
        if (this.listeners.isEmpty()) {
            return this.range = EMPTY_RANGE;
        }
        int positiveX = -1;
        int positiveY = -1;
        int positiveZ = -1;
        int negativeX = 0;
        int negativeY = 0;
        int negativeZ = 0;

        for (NearbyEntityListener listener : this.listeners) {
            Range6Int chunkRange = listener.getChunkRange();
            positiveX = Math.max(chunkRange.positiveX(), positiveX);
            positiveY = Math.max(chunkRange.positiveY(), positiveY);
            positiveZ = Math.max(chunkRange.positiveZ(), positiveZ);
            negativeX = Math.max(chunkRange.negativeX(), negativeX);
            negativeY = Math.max(chunkRange.negativeY(), negativeY);
            negativeZ = Math.max(chunkRange.negativeZ(), negativeZ);

        }
        return this.range = new Range6Int(positiveX, positiveY, positiveZ, negativeX, negativeY, negativeZ);
    }

    @Override
    public void onEntityEnteredRange(Entity entity) {
        for (NearbyEntityListener listener : this.listeners) {
            listener.onEntityEnteredRange(entity);
        }
    }

    @Override
    public void onEntityLeftRange(Entity entity) {
        for (NearbyEntityListener listener : this.listeners) {
            listener.onEntityLeftRange(entity);
        }
    }

    @Override
    public String toString() {
        StringBuilder sublisteners = new StringBuilder();
        String comma = "";
        for (NearbyEntityListener listener : this.listeners) {
            sublisteners.append(comma).append(listener.toString());
            comma = ","; //trick to drop the first comma
        }

        return super.toString() + " with sublisteners: [" + sublisteners + "]";
    }
}
