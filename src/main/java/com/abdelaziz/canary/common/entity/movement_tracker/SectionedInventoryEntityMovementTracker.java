package com.abdelaziz.canary.common.entity.movement_tracker;

import com.abdelaziz.canary.common.util.tuples.WorldSectionBox;
import com.abdelaziz.canary.mixin.util.entity_movement_tracking.PersistentEntitySectionManagerAccessor;
import com.abdelaziz.canary.mixin.util.entity_movement_tracking.ServerLevelAccessor;
import com.abdelaziz.canary.mixin.block.hopper.EntitySectionAccessor;
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
        MovementTrackerCache cache = (MovementTrackerCache) ((PersistentEntitySectionManagerAccessor<?>) ((ServerLevelAccessor) world).getEntityManager()).getSectionStorage();

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
                ClassInstanceMultiMap<S> collection = ((EntitySectionAccessor<S>) this.sortedSections.get(i)).getStorage();

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
