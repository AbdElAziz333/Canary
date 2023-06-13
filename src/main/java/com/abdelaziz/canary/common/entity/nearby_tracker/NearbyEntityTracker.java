package com.abdelaziz.canary.common.entity.nearby_tracker;

import com.abdelaziz.canary.common.util.tuples.Range6Int;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Maintains a collection of all entities within the range of this listener. This allows AI goals to quickly
 * assess nearby entities which match the provided class.
 */
public class NearbyEntityTracker<T extends LivingEntity> implements NearbyEntityListener {
    private final Class<T> clazz;
    private final LivingEntity self;

    private final Reference2LongOpenHashMap<T> nearbyEntities = new Reference2LongOpenHashMap<>(0);
    private final Range6Int chunkBoxRadius;

    public NearbyEntityTracker(Class<T> clazz, LivingEntity self, Vec3i boxRadius) {
        this.clazz = clazz;
        this.self = self;
        this.chunkBoxRadius = new Range6Int(
                1 + SectionPos.blockToSectionCoord(boxRadius.getX()),
                1 + SectionPos.blockToSectionCoord(boxRadius.getY()),
                1 + SectionPos.blockToSectionCoord(boxRadius.getZ()),
                1 + SectionPos.blockToSectionCoord(boxRadius.getX()),
                1 + SectionPos.blockToSectionCoord(boxRadius.getY()),
                1 + SectionPos.blockToSectionCoord(boxRadius.getZ())
        );
    }

    @Override
    public Class<? extends Entity> getEntityClass() {
        return this.clazz;
    }

    /**
     * Gets the closest T (extends LivingEntity) to the center of this tracker that also intersects with the given box and meets the
     * requirements of the targetPredicate.
     * The result may be different from vanilla if there are multiple closest entities.
     *
     * @param box             the box the entities have to intersect
     * @param targetPredicate predicate the entity has to meet
     * @param x
     * @param y
     * @param z
     * @return the closest Entity that meets all requirements (distance, box intersection, predicate, type T)
     */
    public T getClosestEntity(AABB box, TargetingConditions targetPredicate, double x, double y, double z) {
        T nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;

        for (T entity : this.nearbyEntities.keySet()) {
            double distance;
            if (
                    (box == null || box.intersects(entity.getBoundingBox())) &&
                            (distance = entity.distanceToSqr(x, y, z)) <= nearestDistance &&
                            targetPredicate.test(this.self, entity)
            ) {
                if (distance == nearestDistance) {
                    nearest = this.getFirst(nearest, entity);
                } else {
                    nearest = entity;
                }
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    /**
     * Gets the Entity that is processed first in vanilla.
     * @param entity1 one Entity
     * @param entity2 the other Entity
     * @return the Entity that is first in vanilla
     */
    private T getFirst(T entity1, T entity2) {
        if (this.getEntityClass() == Player.class) {
            //Get first in player list
            List<? extends Player> players = this.self.getCommandSenderWorld().players();
            return players.indexOf((Player) entity1) < players.indexOf((Player) entity2) ? entity1 : entity2;
        } else {
            //Get first sorted by chunk section pos as long, then sorted by first added to the chunk section
            //First added to this tracker and first added to the chunk section is equivalent here, because
            //this tracker always tracks complete sections and the entities are added in order
            long pos1 = SectionPos.asLong(entity1.blockPosition());
            long pos2 = SectionPos.asLong(entity2.blockPosition());
            if (pos1 < pos2) {
                return entity1;
            } else if (pos2 < pos1) {
                return entity2;
            } else {
                if (this.nearbyEntities.getLong(entity1) < this.nearbyEntities.getLong(entity2)) {
                    return entity1;
                } else {
                    return entity2;
                }
            }
        }

    }

    @Override
    public String toString() {
        return super.toString() + " for entity class: " + this.clazz.getName() + ", around entity: " + this.self.toString() + " with NBT: " + this.self.saveWithoutId(new CompoundTag());
    }
}