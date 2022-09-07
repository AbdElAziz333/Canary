package com.abdelaziz.canary.common.entity.tracker;

import com.abdelaziz.canary.common.entity.tracker.nearby.NearbyEntityListener;
import com.abdelaziz.canary.common.entity.tracker.nearby.SectionedEntityMovementTracker;
import net.minecraft.world.entity.SectionedEntityCache;

public interface EntityTrackerSection {
    void addListener(NearbyEntityListener listener);

    void removeListener(SectionedEntityCache<?> sectionedEntityCache, NearbyEntityListener listener);

    void addListener(SectionedEntityMovementTracker<?, ?> listener);

    void removeListener(SectionedEntityCache<?> sectionedEntityCache, SectionedEntityMovementTracker<?, ?> listener);

    void updateMovementTimestamps(int notificationMask, long time);

    long[] getMovementTimestampArray();
}
