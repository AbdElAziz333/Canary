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
    abstract AttributeInstance getAttribute(Attribute attribute);

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Redirect(
            method = "tryAddFrost()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getBlockStateOnLegacy()Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private BlockState delayGetBlockState(LivingEntity instance) {
        return null;
    }

    @Redirect(
            method = "tryAddFrost()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"
            )
    )
    private boolean delayAirTest(BlockState instance) {
        return false;
    }

    @Redirect(
            method = "tryAddFrost()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getAttribute(Lnet/minecraft/world/entity/ai/attributes/Attribute;)Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;"
            )
    )
    private AttributeInstance doDelayedBlockStateAirTest(LivingEntity instance, Attribute attribute) {
        //noinspection deprecation
        return this.getBlockStateOnLegacy().isAir() ? null : this.getAttribute(attribute);
    }
}
