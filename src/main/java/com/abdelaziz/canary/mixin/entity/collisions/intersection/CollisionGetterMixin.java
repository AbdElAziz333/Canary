package com.abdelaziz.canary.mixin.entity.collisions.intersection;

import com.abdelaziz.canary.common.entity.LithiumEntityCollisions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Replaces collision testing methods with jumps to our own (faster) entity collision testing code.
 */
@Mixin(CollisionGetter.class)
public interface CollisionGetterMixin {

    /**
     * Checks whether the area is empty from blocks, hard entities and the world border.
     *
     * @reason Only access relevant entity classes, use more efficient block access
     * @author 2No2Name
     */
    @Overwrite
    default boolean isSpaceEmpty(@Nullable Entity entity, AABB box) {
        boolean ret = !LithiumEntityCollisions.doesBoxCollideWithBlocks((CollisionGetter) this, entity, box);

        // If no blocks were collided with, try to check for entity collisions if we can read entities
        if (ret && this instanceof EntityGetter) {
            //needs to include world border collision
            ret = !LithiumEntityCollisions.doesBoxCollideWithHardEntities((EntityGetter) this, entity, box);
        }

        if (ret && entity != null) {
            ret = !LithiumEntityCollisions.doesEntityCollideWithWorldBorder((CollisionGetter) this, entity);
        }

        return ret;
    }
}