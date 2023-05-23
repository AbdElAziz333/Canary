package com.abdelaziz.canary.mixin.calc.deduplicate.player_statistics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    @Shadow public abstract void causeFoodExhaustion(float p_36400_);

    @Shadow public abstract void awardStat(ResourceLocation p_36223_, int p_36224_);

    protected PlayerMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    /**
     * @reason avoid many duplicate calculations
     * @author AbdElAziz
     * */
    @Overwrite
    public void checkMovementStatistics(double d0, double d1, double d2) {
        double d3 = d0 * d0 + d1 * d1 + d2 * d2;
        int round = Math.round((float) Math.sqrt(d3) * 100.0F);

        if (!this.isPassenger()) {
            if (this.isSwimming()) {
                if (round > 0) {
                    this.awardStat(Stats.SWIM_ONE_CM, round);
                    this.causeFoodExhaustion(0.01F * (float)round * 0.01F);
                }
            } else if (this.isEyeInFluid(FluidTags.WATER)) {
                if (round > 0) {
                    this.awardStat(Stats.WALK_UNDER_WATER_ONE_CM, round);
                    this.causeFoodExhaustion(0.01F * (float)round * 0.01F);
                }
            } else if (this.isInWater()) {
                if (round > 0) {
                    this.awardStat(Stats.WALK_ON_WATER_ONE_CM, round);
                    this.causeFoodExhaustion(0.01F * (float)round * 0.01F);
                }
            } else if (this.onClimbable()) {
                if (d1 > 0.0) {
                    this.awardStat(Stats.CLIMB_ONE_CM, (int)Math.round(d1 * 100.0));
                }
            } else if (this.onGround) {
                if (round > 0) {
                    if (this.isSprinting()) {
                        this.awardStat(Stats.SPRINT_ONE_CM, round);
                        this.causeFoodExhaustion(0.1F * (float)round * 0.01F);
                    } else if (this.isCrouching()) {
                        this.awardStat(Stats.CROUCH_ONE_CM, round);
                        this.causeFoodExhaustion(0.0F * (float)round * 0.01F);
                    } else {
                        this.awardStat(Stats.WALK_ONE_CM, round);
                        this.causeFoodExhaustion(0.0F * (float)round * 0.01F);
                    }
                }
            } else if (this.isFallFlying()) {
                this.awardStat(Stats.AVIATE_ONE_CM, round);
            } else {
                if (round > 25) {
                    this.awardStat(Stats.FLY_ONE_CM, round);
                }
            }
        }

    }
}
