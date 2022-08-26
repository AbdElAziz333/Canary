package com.abdelaziz.canary.mixin.ai.nearby_entity_tracking.goals;

import com.abdelaziz.canary.common.entity.tracker.nearby.NearbyEntityListenerProvider;
import com.abdelaziz.canary.common.entity.tracker.nearby.NearbyEntityTracker;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
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

@Mixin(LookAtPlayerGoal.class)
public class LookAtPlayerGoalMixin {
    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    @Final
    protected float range;

    private NearbyEntityTracker<? extends LivingEntity> tracker;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;FFZ)V", at = @At("RETURN"))
    private void init(Mob mob, Class<? extends LivingEntity> targetType, float range, float chance, boolean b, CallbackInfo ci) {
        EntityDimensions dimensions = this.mob.getType().getDimensions();
        double adjustedRange = dimensions.width * 0.5D + this.range + 2D;
        int horizontalRange = Mth.ceil(adjustedRange);
        this.tracker = new NearbyEntityTracker<>(targetType, mob, new Vec3i(horizontalRange, Mth.ceil(dimensions.height + 3 + 2), horizontalRange));

        ((NearbyEntityListenerProvider) mob).addListener(this.tracker);
    }

    @Redirect(
            method = "canUse()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getClosestEntity(Ljava/util/List;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;DDD)Lnet/minecraft/world/entity/LivingEntity;"
            )
    )
    private LivingEntity redirectGetNearestEntity(Level world, List<LivingEntity> entityList, TargetingConditions targetPredicate, LivingEntity entity, double x, double y, double z) {
        return this.tracker.getClosestEntity(this.mob.getBoundingBox().inflate(this.range, 3.0D, this.range), targetPredicate, x, y, z);
    }

    @Redirect(method = "canUse()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <R extends Entity> List<R> redirectGetEntities(Level world, Class<LivingEntity> entityClass, AABB box, Predicate<? super R> predicate) {
        return null;
    }

    @Redirect(
            method = "canUse()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getClosestPlayer(Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/entity/LivingEntity;DDD)Lnet/minecraft/entity/player/PlayerEntity;"
            )
    )
    private Player redirectGetClosestPlayer(Level world, TargetingConditions targetPredicate, LivingEntity entity, double x, double y, double z) {
        return (Player) this.tracker.getClosestEntity(null, targetPredicate, x, y, z);
    }
}
