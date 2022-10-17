package com.abdelaziz.canary.common.entity.tracker.nearby;

import com.abdelaziz.canary.common.util.tuples.WorldSectionBox;
import com.abdelaziz.canary.mixin.ai.nearby_entity_tracking.ServerEntityManagerAccessor;
import com.abdelaziz.canary.mixin.ai.nearby_entity_tracking.ServerWorldAccessor;
import com.abdelaziz.canary.mixin.block.hopper.EntityTrackingSectionAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class SectionedInventoryEntityMovementTracker<S> extends SectionedEntityMovementTracker<Entity, S> {

    public SectionedInventoryEntityMovementTracker(WorldSectionBox entityAccessBox, Class<S> clazz) {
        super(entityAccessBox, clazz);
    }

    public static <S> SectionedInventoryEntityMovementTracker<S> registerAt(ServerLevel world, AABB interactionArea, Class<S> clazz) {
        MovementTrackerCache cache = (MovementTrackerCache) ((ServerEntityManagerAccessor<?>) ((ServerWorldAccessor) world).getEntityManager()).getSectionStorage();

        WorldSectionBox worldSectionBox = WorldSectionBox.entityAccessBox(world, interactionArea);
        SectionedInventoryEntityMovementTracker<S> tracker = new SectionedInventoryEntityMovementTracker<>(worldSectionBox, clazz);
        tracker = cache.deduplicate(tracker);

        tracker.register(world);
        return tracker;
    }

    public List<S> getEntities(AABB box) {
        ArrayList<S> entities = new ArrayList<>();
        for (int i = 0; i < this.sortedSections.size(); i++) {
            if (this.sectionVisible[i]) {
                //noinspection unchecked
                ClassInstanceMultiMap<S> collection = ((EntityTrackingSectionAccessor<S>) this.sortedSections.get(i)).getStorage();

                for (S entity : collection.find(this.clazz)) {
                    Entity inventoryEntity = (Entity) entity;
                    if (inventoryEntity.isAlive() && inventoryEntity.getBoundingBox().intersects(box)) {
                        entities.add(entity);
                    }
                }
            }
        }
        return entities;
    }
}
