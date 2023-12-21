package com.abdelaziz.canary.common.entity.movement_tracker;

public interface MovementTrackerCache {
    void remove(SectionedEntityMovementTracker<?, ?> tracker);

    <S extends SectionedEntityMovementTracker<?, ?>> S deduplicate(S tracker);
}
