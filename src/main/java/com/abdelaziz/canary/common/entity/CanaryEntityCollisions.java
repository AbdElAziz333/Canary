package com.abdelaziz.canary.common.entity;

import com.abdelaziz.canary.common.entity.movement.ChunkAwareBlockCollisionSweeper;
import com.abdelaziz.canary.common.util.Pos;
import com.abdelaziz.canary.common.world.WorldHelper;
import com.google.common.collect.AbstractIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CanaryEntityCollisions {
    public static final double EPSILON = 1.0E-7D;

    /**
     * [VanillaCopy] CollisionGetter#getBlockCollisions(Entity, AABB)
     * This is a much, much faster implementation which uses simple collision testing against full-cube block shapes.
     * Checks against the world border are replaced with our own optimized functions which do not go through the
     * VoxelShape system.
     */
    public static List<VoxelShape> getBlockCollisions(Level level, Entity entity, AABB box) {
        return new ChunkAwareBlockCollisionSweeper(level, entity, box).collectAll();
    }

    /***
     * @return True if the box (possibly that of an entity's) collided with any blocks
     */
    public static boolean doesBoxCollideWithBlocks(Level level, Entity entity, AABB box) {
        final ChunkAwareBlockCollisionSweeper sweeper = new ChunkAwareBlockCollisionSweeper(level, entity, box);

        final VoxelShape shape = sweeper.computeNext();

        return shape != null && !shape.isEmpty();
    }

    /**
     * @return True if the box (possibly that of an entity's) collided with any other hard entities
     */
    public static boolean doesBoxCollideWithHardEntities(EntityGetter view, Entity entity, AABB box) {
        if (isBoxEmpty(box)) {
            return false;
        }

        return getEntityWorldBorderCollisionIterable(view, entity, box.inflate(EPSILON), false).iterator().hasNext();
    }

    /**
     * Iterates entity and world border collision boxes.
     */
    public static List<VoxelShape> getEntityWorldBorderCollisions(Level world, Entity entity, AABB box, boolean includeWorldBorder) {
        if (isBoxEmpty(box)) {
            return Collections.emptyList();
        }
        ArrayList<VoxelShape> shapes = new ArrayList<>();
        Iterable<VoxelShape> collisions = getEntityWorldBorderCollisionIterable(world, entity, box.inflate(EPSILON), includeWorldBorder);
        for (VoxelShape shape : collisions) {
            shapes.add(shape);
        }
        return shapes;
    }

    /**
     * [VanillaCopy] EntityView#getEntityCollisions
     * Re-implements the function named above without stream code or unnecessary allocations. This can provide a small
     * boost in some situations (such as heavy entity crowding) and reduces the allocation rate significantly.
     */
    public static Iterable<VoxelShape> getEntityWorldBorderCollisionIterable(EntityGetter view, Entity entity, AABB box, boolean includeWorldBorder) {
        assert !includeWorldBorder || entity != null;
        return new Iterable<>() {
            private List<Entity> entityList;
            private int nextFilterIndex;

            @NotNull
            @Override
            public Iterator<VoxelShape> iterator() {
                return new AbstractIterator<>() {
                    int index = 0;
                    boolean consumedWorldBorder = false;

                    @Override
                    protected VoxelShape computeNext() {
                        //Initialize list that is shared between multiple iterators as late as possible
                        if (entityList == null) {
                            /*
                             * In case entity's class is overriding Entity#collidesWith(Entity), all types of entities may be (=> are assumed to be) required.
                             * Otherwise only get entities that override Entity#isCollidable(), as other entities cannot collide.
                             */
                            entityList = WorldHelper.getEntitiesForCollision(view, box, entity);
                            nextFilterIndex = 0;
                        }
                        List<Entity> list = entityList;
                        Entity otherEntity;
                        do {
                            if (this.index >= list.size()) {
                                //get the world border at the end
                                if (includeWorldBorder && !this.consumedWorldBorder) {
                                    this.consumedWorldBorder = true;
                                    WorldBorder worldBorder = entity.level().getWorldBorder();
                                    if (!isWithinWorldBorder(worldBorder, box) && isWithinWorldBorder(worldBorder, entity.getBoundingBox())) {
                                        return worldBorder.getCollisionShape();
                                    }
                                }
                                return this.endOfData();
                            }

                            otherEntity = list.get(this.index);
                            if (this.index >= nextFilterIndex) {
                                /*
                                 * {@link Entity#isCollidable()} returns false by default, designed to be overridden by
                                 * entities whose collisions should be "hard" (boats and shulkers, for now).
                                 *
                                 * {@link Entity#collidesWith(Entity)} only allows hard collisions if the calling entity is not riding
                                 * otherEntity as a vehicle.
                                 */
                                if (entity == null) {
                                    if (!otherEntity.canBeCollidedWith()) {
                                        otherEntity = null;
                                    }
                                } else if (!entity.canCollideWith(otherEntity)) {
                                    otherEntity = null;
                                }
                                nextFilterIndex++;
                            }
                            this.index++;
                        } while (otherEntity == null);

                        return Shapes.create(otherEntity.getBoundingBox());
                    }
                };
            }
        };
    }

    /**
     * This provides a faster check for seeing if an entity is within the world border as it avoids going through
     * the slower shape system.
     *
     * @return True if the {@param box} is fully within the {@param border}, otherwise false.
     */
    public static boolean isWithinWorldBorder(WorldBorder border, AABB box) {
        double wboxMinX = Math.floor(border.getMinX());
        double wboxMinZ = Math.floor(border.getMinZ());

        double wboxMaxX = Math.ceil(border.getMaxX());
        double wboxMaxZ = Math.ceil(border.getMaxZ());

        return box.minX >= wboxMinX && box.minX <= wboxMaxX && box.minZ >= wboxMinZ && box.minZ <= wboxMaxZ &&
                box.maxX >= wboxMinX && box.maxX <= wboxMaxX && box.maxZ >= wboxMinZ && box.maxZ <= wboxMaxZ;
    }


    private static boolean isBoxEmpty(AABB box) {
        return box.getSize() <= EPSILON;
    }

    public static boolean doesEntityCollideWithWorldBorder(CollisionGetter collisionView, Entity entity) {
        if (isWithinWorldBorder(collisionView.getWorldBorder(), entity.getBoundingBox())) {
            return false;
        } else {
            VoxelShape worldBorderShape = getWorldBorderCollision(collisionView, entity);
            return worldBorderShape != null && Shapes.joinIsNotEmpty(worldBorderShape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND);
        }
    }

    public static VoxelShape getWorldBorderCollision(CollisionGetter collisionView, Entity entity) {
        AABB box = entity.getBoundingBox();
        WorldBorder worldBorder = collisionView.getWorldBorder();
        return worldBorder.isInsideCloseToBorder(entity, box) ? worldBorder.getCollisionShape() : null;
    }

    public static VoxelShape getCollisionShapeBelowEntity(Level world, @Nullable Entity entity, AABB entityBoundingBox) {
        int x = Mth.floor(entityBoundingBox.minX + (entityBoundingBox.maxX - entityBoundingBox.minX) / 2);
        int y = Mth.floor(entityBoundingBox.minY);
        int z = Mth.floor(entityBoundingBox.minZ + (entityBoundingBox.maxZ - entityBoundingBox.minZ) / 2);
        if (world.isOutsideBuildHeight(y)) {
            return null;
        }
        ChunkAccess chunk = world.getChunk(Pos.ChunkCoord.fromBlockCoord(x), Pos.ChunkCoord.fromBlockCoord(z), ChunkStatus.FULL, false);
        if (chunk != null) {
            LevelChunkSection cachedChunkSection = chunk.getSections()[Pos.SectionYIndex.fromBlockCoord(world, y)];
            return cachedChunkSection.getBlockState(x & 15, y & 15, z & 15).getCollisionShape(world, new BlockPos(x, y, z), entity == null ? CollisionContext.empty() : CollisionContext.of(entity));
        }
        return null;
    }
}
