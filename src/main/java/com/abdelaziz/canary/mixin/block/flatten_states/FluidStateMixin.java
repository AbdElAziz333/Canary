package com.abdelaziz.canary.mixin.block.flatten_states;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.stream.Stream;

@Mixin(FluidState.class)
public abstract class FluidStateMixin {
    float explosionsResistance;
    boolean isEmpty;
    boolean isRandomlyTicking;
    ParticleOptions dripParticles;
    Stream<TagKey<Fluid>> tags;

    @Shadow
    public abstract Fluid getType();

    @Inject(method = "<init>(Lnet/minecraft/world/level/material/Fluid;Lcom/google/common/collect/ImmutableMap;Lcom/mojang/serialization/MapCodec;)V", at = @At("RETURN"))
    private void initFluidCache(Fluid fluid, ImmutableMap<Property<?>, Comparable<?>> propertyMap, MapCodec<FluidState> codec, CallbackInfo ci) {
        explosionsResistance = this.getType().getExplosionResistance();
        isEmpty = this.getType().isEmpty();
        isRandomlyTicking = this.getType().isRandomlyTicking();
        dripParticles = this.getType().getDripParticle();
        tags = this.getType().builtInRegistryHolder().tags();
    }

    /**
     * @reason Use cached property
     * @author AbdElAziz
     * */
    @Overwrite
    @Deprecated
    public float getExplosionResistance() {
        return explosionsResistance;
    }

    /**
     * @reason Use cached property
     * @author Maity
     */
    @Overwrite
    public boolean isEmpty() {
        return this.isEmpty;
    }

    /**
     * @reason Use cached property
     * @author AbdElAziz
     * */
    @Overwrite
    public boolean isRandomlyTicking() {
        return isRandomlyTicking;
    }

    /**
     * @reason Use cached property
     * @author AbdElAziz
     * */
    @Nullable
    @Overwrite
    public ParticleOptions getDripParticle() {
        return dripParticles;
    }

    /**
     * @reason Use cached property
     * @author AbdElAziz
     * */
    @Overwrite
    public Stream<TagKey<Fluid>> getTags() {
        return tags;
    }
}