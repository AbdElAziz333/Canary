package com.abdelaziz.canary.mixin.entity.fast_powder_snow_check;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow
    public @Nullable
    abstract AttributeInstance getAttributeInstance(Attribute attribute);

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Redirect(
            method = "addPowderSnowSlowIfNeeded()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getLandingBlockState()Lnet/minecraft/block/BlockState;"
            )
    )
    private BlockState delayGetBlockState(LivingEntity instance) {
        return null;
    }

    @Redirect(
            method = "addPowderSnowSlowIfNeeded()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"
            )
    )
    private boolean delayAirTest(BlockState instance) {
        return false;
    }

    @Redirect(
            method = "addPowderSnowSlowIfNeeded()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeInstance(Lnet/minecraft/entity/attribute/EntityAttribute;)Lnet/minecraft/entity/attribute/EntityAttributeInstance;"
            )
    )
    private AttributeInstance doDelayedBlockStateAirTest(LivingEntity instance, Attribute attribute) {
        return this.getBlockStateOn().isAir() ? null : this.getAttributeInstance(attribute);
    }
}
