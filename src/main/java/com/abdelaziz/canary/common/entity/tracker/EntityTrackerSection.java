package com.abdelaziz.canary.common.entity.tracker;

import com.abdelaziz.canary.common.entity.tracker.nearby.NearbyEntityListener;
import com.abdelaziz.canary.common.entity.tracker.nearby.SectionedEntityMovementTracker;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;

public interface EntityTrackerSection {
    void addListener(NearbyEntityListener listener);

    void removeListener(EntitySectionStorage<?> sectionedEntityCache, NearbyEntityListener listener);

    void addListener(SectionedEntityMovementTracker<?, ?> listener);

    void removeListener(EntitySectionStorage<?> sectionedEntityCache, SectionedEntityMovementTracker<?, ?> listener);

    void trackEntityMovement(int notificationMask, long time);

    long[] getMovementTimestampArray();

    long getChangeTime(int trackedClass);

    <S, E extends EntityAccess> void listenToMovementOnce(SectionedEntityMovementTracker<E, S> listener, int trackedClass);

    <S, E extends EntityAccess> void removeListenToMovementOnce(SectionedEntityMovementTracker<E, S> listener, int trackedClass);
}
