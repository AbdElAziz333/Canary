package com.abdelaziz.canary.mixin.util.entity_movement_tracking;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import com.abdelaziz.canary.common.entity.movement_tracker.MovementTrackerCache;
import com.abdelaziz.canary.common.entity.movement_tracker.SectionedEntityMovementTracker;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntitySectionStorage.class)
public class EntitySectionStorageMixin implements MovementTrackerCache {

    private final Object2ReferenceOpenHashMap<SectionedEntityMovementTracker<?, ?>, SectionedEntityMovementTracker<?, ?>> sectionEntityMovementTrackers = new Object2ReferenceOpenHashMap<>();

    @Override
    public void remove(SectionedEntityMovementTracker<?, ?> tracker) {
        this.sectionEntityMovementTrackers.remove(tracker);
    }

    @Override
    public <S extends SectionedEntityMovementTracker<?, ?>> S deduplicate(S tracker) {
        //noinspection unchecked
        S storedTracker = (S) this.sectionEntityMovementTrackers.putIfAbsent(tracker, tracker);
        return storedTracker == null ? tracker : storedTracker;
    }
}