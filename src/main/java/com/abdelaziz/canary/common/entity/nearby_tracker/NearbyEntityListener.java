package com.abdelaziz.canary.common.entity.nearby_tracker;

import com.abdelaziz.canary.common.util.tuples.Range6Int;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public interface NearbyEntityListener {
    Range6Int EMPTY_RANGE = new Range6Int(0, 0, 0, -1, -1, -1);

    /**
     * Calls the callbacks for the chunk coordinates that this listener is leaving and entering
     */
    default void updateChunkRegistrations(EntitySectionStorage<? extends EntityAccess> entityCache, SectionPos prevCenterPos, Range6Int prevChunkRange, SectionPos newCenterPos, Range6Int newChunkRange) {
        if (prevChunkRange == EMPTY_RANGE && newChunkRange == EMPTY_RANGE) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        BoundingBox after = newCenterPos == null ? null : new BoundingBox(newCenterPos.getX() - newChunkRange.negativeX(), newCenterPos.getY() - newChunkRange.negativeY(), newCenterPos.getZ() - newChunkRange.negativeZ(), newCenterPos.getX() + newChunkRange.positiveX(), newCenterPos.getY() + newChunkRange.positiveY(), newCenterPos.getZ() + newChunkRange.positiveZ());
        BoundingBox before = prevCenterPos == null ? null : new BoundingBox(prevCenterPos.getX() - prevChunkRange.negativeX(), prevCenterPos.getY() - prevChunkRange.negativeY(), prevCenterPos.getZ() - prevChunkRange.negativeZ(), prevCenterPos.getX() + prevChunkRange.positiveX(), prevCenterPos.getY() + prevChunkRange.positiveY(), prevCenterPos.getZ() + prevChunkRange.positiveZ());
        if (before != null) {
            for (int x = before.minX(); x <= before.maxX(); x++) {
                for (int y = before.minY(); y <= before.maxY(); y++) {
                    for (int z = before.minZ(); z <= before.maxZ(); z++) {
                        if (after == null || !after.isInside(pos.set(x, y, z))) {
                            long sectionPos = SectionPos.asLong(x, y, z);
                            EntitySection<? extends EntityAccess> trackingSection = entityCache.getOrCreateSection(sectionPos);
                            ((NearbyEntityListenerSection) trackingSection).removeListener(entityCache, this);
                            if (trackingSection.isEmpty()) {
                                entityCache.remove(sectionPos);
                            }
                        }
                    }
                }
            }
        }
        if (after != null) {
            for (int x = after.minX(); x <= after.maxX(); x++) {
                for (int y = after.minY(); y <= after.maxY(); y++) {
                    for (int z = after.minZ(); z <= after.maxZ(); z++) {
                        if (before == null || !before.isInside(pos.set(x, y, z))) {
                            ((NearbyEntityListenerSection) entityCache.getOrCreateSection(SectionPos.asLong(x, y, z))).addListener(this);
                        }
                    }
                }
            }
        }
    }
    default void removeFromAllChunksInRange(EntitySectionStorage<? extends EntityAccess> entityCache, SectionPos prevCenterPos) {
        this.updateChunkRegistrations(entityCache, prevCenterPos, this.getChunkRange(), null, EMPTY_RANGE);
    }

    default void addToAllChunksInRange(EntitySectionStorage<? extends EntityAccess> entityCache, SectionPos newCenterPos) {
        this.updateChunkRegistrations(entityCache, null, EMPTY_RANGE, newCenterPos, this.getChunkRange());
    }

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

    /**
     * Method to add all entities in the iteration order of the chunk section. This order is relevant and necessary
     * to keep vanilla parity.
     *
     * @param <T>                   the type of the Entities in the collection
     * @param entityTrackingSection the section the entities are in
     * @param collection            the collection of Entities that entered the range of this listener
     */
    default <T> void onSectionEnteredRange(Object entityTrackingSection, ClassInstanceMultiMap<T> collection) {
        for (Entity entity : collection.find(this.getEntityClass())) {
            this.onEntityEnteredRange(entity);
        }
    }

    default <T> void onSectionLeftRange(Object entityTrackingSection, ClassInstanceMultiMap<T> collection) {
        for (Entity entity : collection.find(this.getEntityClass())) {
            this.onEntityLeftRange(entity);
        }
    }

}
