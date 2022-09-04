package com.abdelaziz.canary.mixin.ai.nearby_entity_tracking;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.lithium.common.entity.tracker.nearby.MovementTrackerCache;
import me.jellysquid.mods.lithium.common.entity.tracker.nearby.SectionedEntityMovementTracker;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SectionedEntityCache.class)
public class SectionedEntityCacheMixin<T extends EntityLike> implements MovementTrackerCache {

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
