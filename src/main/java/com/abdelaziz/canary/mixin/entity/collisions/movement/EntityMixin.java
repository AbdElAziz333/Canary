package com.abdelaziz.canary.mixin.entity.collisions.movement;

import com.abdelaziz.canary.common.entity.CanaryEntityCollisions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(Entity.class)
public class EntityMixin {

    @Redirect(
            method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
            )
    )
    private List<VoxelShape> getEntitiesLater(Level world, Entity entity, AABB box) {
        return List.of();
    }

    @Redirect(
            method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"
            ),
            require = 5
    )
    private Vec3 adjustMovementForCollisionsGetEntitiesLater(@Nullable Entity entity, Vec3 movement, AABB entityBoundingBox, Level world, List<VoxelShape> collisions) {
        return canaryCollideMultiAxisMovement(entity, movement, entityBoundingBox, world, true, collisions);
    }

    /**
     * @author 2No2Name
     * @reason Replace with optimized implementation
     */
    @Overwrite
    public static Vec3 collideBoundingBox(@Nullable Entity entity, Vec3 movement, AABB entityBoundingBox, Level world, List<VoxelShape> collisions) {
        return canaryCollideMultiAxisMovement(entity, movement, entityBoundingBox, world, false, collisions);
    }

    private static Vec3 canaryCollideMultiAxisMovement(@Nullable Entity entity, Vec3 movement, AABB entityBoundingBox, Level world, boolean getEntityCollisions, List<VoxelShape> otherCollisions) {
        //vanilla order: entities, worldborder, blocks. It is unknown whether changing this order changes the result regarding the confusing 1e-7 VoxelShape margin behavior. Not yet investigated
        double velX = movement.x;
        double velY = movement.y;
        double velZ = movement.z;
        boolean isVerticalOnly = velX == 0 && velZ == 0;
        AABB movementSpace;
        if (isVerticalOnly) {
            if (velY < 0) {
                //Check block directly below center of entity first
                VoxelShape voxelShape = CanaryEntityCollisions.getCollisionShapeBelowEntity(world, entity, entityBoundingBox);
                if (voxelShape != null) {
                    double v = voxelShape.collide(Direction.Axis.Y, entityBoundingBox, velY);
                    if (v == 0) {
                        return Vec3.ZERO;
                    }
                }
                //Reduced collision volume optimization for entities that are just standing around
                movementSpace = new AABB(entityBoundingBox.minX, entityBoundingBox.minY + velY, entityBoundingBox.minZ, entityBoundingBox.maxX, entityBoundingBox.minY, entityBoundingBox.maxZ);
            } else {
                movementSpace = new AABB(entityBoundingBox.minX, entityBoundingBox.maxY, entityBoundingBox.minZ, entityBoundingBox.maxX, entityBoundingBox.maxY + velY, entityBoundingBox.maxZ);
            }
        } else {
            movementSpace = entityBoundingBox.expandTowards(movement);
        }

        List<VoxelShape> blockCollisions = CanaryEntityCollisions.getBlockCollisions(world, entity, movementSpace);
        List<VoxelShape> entityWorldBorderCollisions = null;

        if (velY != 0.0) {
            velY = Shapes.collide(Direction.Axis.Y, entityBoundingBox, blockCollisions, velY);
            if (velY != 0.0) {
                if (!otherCollisions.isEmpty()) {
                    velY = Shapes.collide(Direction.Axis.Y, entityBoundingBox, otherCollisions, velY);
                }
                if (velY != 0.0 && getEntityCollisions) {
                    entityWorldBorderCollisions = CanaryEntityCollisions.getEntityWorldBorderCollisions(world, entity, movementSpace, entity != null);
                    velY = Shapes.collide(Direction.Axis.Y, entityBoundingBox, entityWorldBorderCollisions, velY);
                }
                if (velY != 0.0) {
                    entityBoundingBox = entityBoundingBox.move(0.0, velY, 0.0);
                }
            }
        }

        boolean velXSmallerVelZ = Math.abs(velX) < Math.abs(velZ);

        if (velXSmallerVelZ) {
            velZ = Shapes.collide(Direction.Axis.Z, entityBoundingBox, blockCollisions, velZ);
            if (velZ != 0.0) {

                if (!otherCollisions.isEmpty()) {
                    velZ = Shapes.collide(Direction.Axis.Z, entityBoundingBox, otherCollisions, velZ);
                }

                if (velZ != 0.0 && getEntityCollisions) {
                    if (entityWorldBorderCollisions == null) {
                        entityWorldBorderCollisions = CanaryEntityCollisions.getEntityWorldBorderCollisions(world, entity, movementSpace, entity != null);
                    }

                    velZ = Shapes.collide(Direction.Axis.Z, entityBoundingBox, entityWorldBorderCollisions, velZ);
                }

                if (velZ != 0.0) {
                    entityBoundingBox = entityBoundingBox.move(0.0, 0.0, velZ);
                }
            }
        }

        if (velX != 0.0) {
            velX = Shapes.collide(Direction.Axis.X, entityBoundingBox, blockCollisions, velX);
            if (velX != 0.0) {
                if (!otherCollisions.isEmpty()) {
                    velX = Shapes.collide(Direction.Axis.X, entityBoundingBox, otherCollisions, velX);
                }
                if (velX != 0.0 && getEntityCollisions) {
                    if (entityWorldBorderCollisions == null) {
                        entityWorldBorderCollisions = CanaryEntityCollisions.getEntityWorldBorderCollisions(world, entity, movementSpace, entity != null);
                    }

                    velX = Shapes.collide(Direction.Axis.X, entityBoundingBox, entityWorldBorderCollisions, velX);
                }
                if (velX != 0.0) {
                    entityBoundingBox = entityBoundingBox.move(velX, 0.0, 0.0);
                }
            }
        }

        if (!velXSmallerVelZ && velZ != 0.0) {
            velZ = Shapes.collide(Direction.Axis.Z, entityBoundingBox, blockCollisions, velZ);
            if (velZ != 0.0) {
                if (!otherCollisions.isEmpty()) {
                    velZ = Shapes.collide(Direction.Axis.Z, entityBoundingBox, otherCollisions, velZ);
                }
                if (velZ != 0.0 && getEntityCollisions) {
                    if (entityWorldBorderCollisions == null) {
                        entityWorldBorderCollisions = CanaryEntityCollisions.getEntityWorldBorderCollisions(world, entity, movementSpace, entity != null);
                    }

                    velZ = Shapes.collide(Direction.Axis.Z, entityBoundingBox, entityWorldBorderCollisions, velZ);
                }
            }
        }
        return new Vec3(velX, velY, velZ);
    }
}