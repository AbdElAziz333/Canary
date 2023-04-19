package com.abdelaziz.canary.mixin.ai.nearby_entity_tracking.goals;

import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityListenerProvider;
import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityTracker;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

@Mixin(AvoidEntityGoal.class)
public class AvoidEntityGoalMixin<T extends LivingEntity> {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    @Final
    protected float maxDist;

    private NearbyEntityTracker<T> tracker;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/PathfinderMob;Ljava/lang/Class;Ljava/util/function/Predicate;FDDLjava/util/function/Predicate;)V", at = @At("RETURN"))
    private void init(PathfinderMob mob, Class<T> fleeFromType, Predicate<LivingEntity> predicate, float distance, double slowSpeed, double fastSpeed, Predicate<LivingEntity> predicate2, CallbackInfo ci) {
        EntityDimensions dimensions = this.mob.getType().getDimensions();
        double adjustedRange = dimensions.width * 0.5D + this.maxDist + 2D;
        int horizontalRange = Mth.ceil(adjustedRange);
        this.tracker = new NearbyEntityTracker<>(fleeFromType, mob, new Vec3i(horizontalRange, Mth.ceil(dimensions.height + 3 + 2), horizontalRange));

        ((NearbyEntityListenerProvider) mob).addListener(this.tracker);
    }

    @Redirect(
            method = "canUse()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getNearestEntity(Ljava/util/List;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;DDD)Lnet/minecraft/world/entity/LivingEntity;"
            )
    )
    private T redirectGetNearestEntity(Level world, List<? extends T> entityList, TargetingConditions targetPredicate, LivingEntity entity, double x, double y, double z) {
        return this.tracker.getClosestEntity(this.mob.getBoundingBox().inflate(this.maxDist, 3.0D, this.maxDist), targetPredicate, x, y, z);
    }

    @Redirect(method = "canUse()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <R extends Entity> List<R> redirectGetEntities(Level world, Class<T> entityClass, AABB box, Predicate<? super R> predicate) {
        return null;
    }
}
