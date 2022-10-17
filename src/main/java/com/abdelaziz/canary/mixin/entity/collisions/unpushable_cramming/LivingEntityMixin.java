package com.abdelaziz.canary.mixin.entity.collisions.unpushable_cramming;

import com.abdelaziz.canary.common.entity.pushable.BlockCachingEntity;
import com.abdelaziz.canary.common.entity.pushable.EntityPushablePredicate;
import com.abdelaziz.canary.common.world.ClimbingMobCachingSection;
import com.abdelaziz.canary.common.world.WorldHelper;
import com.google.common.base.Predicates;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements BlockCachingEntity {

    boolean updateClimbingMobCachingSectionOnChange;

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Redirect(
            method = "pushEntities()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private List<Entity> getOtherPushableEntities(Level world, @Nullable Entity except, AABB box, Predicate<? super Entity> predicate) {
        //noinspection Guava
        if (predicate == Predicates.alwaysFalse()) {
            return Collections.emptyList();
        }
        if (predicate instanceof EntityPushablePredicate<?> entityPushablePredicate) {
            EntitySectionStorage<Entity> cache = WorldHelper.getEntityCacheOrNull(world);
            if (cache != null) {
                //noinspection unchecked
                return WorldHelper.getPushableEntities(world, cache, except, box, (EntityPushablePredicate<? super Entity>) entityPushablePredicate);
            }
        }
        return world.getEntities(except, box, predicate);
    }

    @Override
    public void canarySetClimbingMobCachingSectionUpdateBehavior(boolean listenForCachedBlockChanges) {
        this.updateClimbingMobCachingSectionOnChange = listenForCachedBlockChanges;
    }

    @Override
    public void canaryOnBlockCacheDeleted() {
        if (this.updateClimbingMobCachingSectionOnChange) {
            this.updateClimbingMobCachingSection(null);
        }
    }


    @Override
    public void canaryOnBlockCacheSet(BlockState newState) {
        if (this.updateClimbingMobCachingSectionOnChange) {
            this.updateClimbingMobCachingSection(newState);
        }
    }

    private void updateClimbingMobCachingSection(BlockState newState) {
        EntitySectionStorage<Entity> entityCacheOrNull = WorldHelper.getEntityCacheOrNull(this.level);
        if (entityCacheOrNull != null) {
            EntitySection<Entity> trackingSection = entityCacheOrNull.getSection(SectionPos.asLong(this.blockPosition()));
            if (trackingSection != null) {
                ((ClimbingMobCachingSection) trackingSection).onEntityModifiedCachedBlock(this, newState);
            } else {
                this.updateClimbingMobCachingSectionOnChange = false;
            }
        }
    }
}
