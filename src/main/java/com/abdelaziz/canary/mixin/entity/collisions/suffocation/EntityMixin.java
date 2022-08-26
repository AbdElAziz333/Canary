package com.abdelaziz.canary.mixin.entity.collisions.suffocation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract Vec3 getEyePos();

    @Shadow
    public Level world;

    @Shadow
    public boolean noClip;

    @Shadow
    private EntityDimensions dimensions;

    /**
     * @author 2No2Name
     * @reason Avoid stream code, use optimized chunk section iteration order
     */
    @Overwrite
    public boolean isInsideWall() {
        // [VanillaCopy] The whole method functionality including bug below. Cannot use ChunkAwareBlockCollisionSweeper due to ignoring of oversized blocks
        if (this.noClip) {
            return false;
        }
        Vec3 eyePos = this.getEyePos();
        double suffocationRadius = Math.abs((double) (this.dimensions.width * 0.8f) / 2.0);

        double suffocationMinX = eyePos.x - suffocationRadius;
        double suffocationMinY = eyePos.y - 5.0E-7;
        double suffocationMinZ = eyePos.z - suffocationRadius;
        double suffocationMaxX = eyePos.x + suffocationRadius;
        double suffocationMaxY = eyePos.y + 5.0E-7;
        double suffocationMaxZ = eyePos.z + suffocationRadius;
        int minX = Mth.floor(suffocationMinX);
        int minY = Mth.floor(suffocationMinY);
        int minZ = Mth.floor(suffocationMinZ);
        int maxX = Mth.floor(suffocationMaxX);
        int maxY = Mth.floor(suffocationMaxY);
        int maxZ = Mth.floor(suffocationMaxZ);

        Level world = this.world;
        //skip getting blocks when the entity is outside the world height
        //also avoids infinite loop with entities below y = Integer.MIN_VALUE (some modded servers do that)
        if (world.getMinBuildHeight() > maxY || world.getMaxBuildHeight() < minY) {
            return false;
        }

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        VoxelShape suffocationShape = null;

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    blockPos.set(x, y, z);
                    BlockState blockState = world.getBlockState(blockPos);
                    if (!blockState.isAir() && blockState.isSuffocating(this.world, blockPos)) {
                        if (suffocationShape == null) {
                            suffocationShape = Shapes.create(new AABB(suffocationMinX, suffocationMinY, suffocationMinZ, suffocationMaxX, suffocationMaxY, suffocationMaxZ));
                        }
                        if (Shapes.joinIsNotEmpty(blockState.getCollisionShape(this.world, blockPos).
                                        move(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                                suffocationShape, BooleanOp.AND)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
