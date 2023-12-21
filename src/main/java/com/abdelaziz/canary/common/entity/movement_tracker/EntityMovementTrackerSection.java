package com.abdelaziz.canary.common.entity.movement_tracker;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;

public interface EntityMovementTrackerSection {
    void addListener(SectionedEntityMovementTracker<?, ?> listener);

    void removeListener(EntitySectionStorage<?> sectionedEntityCache, SectionedEntityMovementTracker<?, ?> listener);

    void trackEntityMovement(int notificationMask, long time);

    long getChangeTime(int trackedClass);

    <S, E extends EntityAccess> void listenToMovementOnce(SectionedEntityMovementTracker<E, S> listener, int trackedClass);

    <S, E extends EntityAccess> void removeListenToMovementOnce(SectionedEntityMovementTracker<E, S> listener, int trackedClass);
}
