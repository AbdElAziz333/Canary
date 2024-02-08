package com.abdelaziz.canary.common.entity.movement_tracker;

import com.abdelaziz.canary.common.util.collections.BucketedList;
import com.abdelaziz.canary.common.util.tuples.WorldSectionBox;
import com.abdelaziz.canary.mixin.util.accessors.PersistentEntitySectionManagerAccessor;
import com.abdelaziz.canary.mixin.util.accessors.ServerLevelAccessor;
import com.abdelaziz.canary.mixin.util.accessors.EntitySectionAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SectionedItemEntityMovementTracker<S extends Entity> extends SectionedEntityMovementTracker<Entity, S> {

    public SectionedItemEntityMovementTracker(WorldSectionBox worldSectionBox, Class<S> clazz) {
        super(worldSectionBox, clazz);
    }

    public static <S extends Entity> SectionedItemEntityMovementTracker<S> registerAt(ServerLevel world, AABB encompassingBox, Class<S> clazz) {
        MovementTrackerCache cache = (MovementTrackerCache) ((PersistentEntitySectionManagerAccessor<?>) ((ServerLevelAccessor) world).getEntityManager()).getSectionStorage();

        WorldSectionBox worldSectionBox = WorldSectionBox.entityAccessBox(world, encompassingBox);
        SectionedItemEntityMovementTracker<S> tracker = new SectionedItemEntityMovementTracker<>(worldSectionBox, clazz);
        tracker = cache.deduplicate(tracker);

        tracker.register(world);
        return tracker;
    }

    public List<S> getEntities(AABB[] areas) {
        int numBoxes = areas.length - 1;
        BucketedList<S> entities = new BucketedList<>(numBoxes);
        AABB encompassingBox = areas[numBoxes];
        for (int sectionIndex = 0; sectionIndex < this.sortedSections.size(); sectionIndex++) {
            if (this.sectionVisible[sectionIndex]) {
                //noinspection unchecked
                ClassInstanceMultiMap<S> collection = ((EntitySectionAccessor<S>) this.sortedSections.get(sectionIndex)).getStorage();

                for (S entity : collection.find(this.clazz)) {
                    if (entity.isAlive()) {
                        AABB entityBoundingBox = entity.getBoundingBox();
                        //even though there are usually only two boxes to check, checking the encompassing box only will be faster in most cases
                        //In vanilla the number of boxes checked is always 2. Here it is 1 (miss) and 2-3 (hit)
                        if (entityBoundingBox.intersects(encompassingBox)) {
                            for (int j = 0; j < numBoxes; j++) {
                                if (entityBoundingBox.intersects(areas[j])) {
                                    entities.addToBucket(j, entity);
                                    //Only add each entity once. A hopper cannot pick up from the entity twice anyways.
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return entities;
    }
}
