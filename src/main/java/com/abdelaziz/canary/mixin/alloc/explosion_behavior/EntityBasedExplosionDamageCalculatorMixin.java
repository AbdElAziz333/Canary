package com.abdelaziz.canary.mixin.alloc.explosion_behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.swing.text.html.BlockView;
import java.util.Optional;

@Mixin(EntityBasedExplosionDamageCalculator.class)
public class EntityBasedExplosionDamageCalculatorMixin extends ExplosionDamageCalculator {
    @Shadow
    @Final
    private Entity entity;

    /**
     * @author 2No2Name
     * @reason avoid lambda and optional allocation
     */
    @Overwrite
    public Optional<Float> getBlastResistance(Explosion explosion, BlockGetter world, BlockPos pos, BlockState blockState, FluidState fluidState) {
        Optional<Float> optionalBlastResistance = super.getBlockExplosionResistance(explosion, world, pos, blockState, fluidState);
        if (optionalBlastResistance.isPresent()) {
            float blastResistance = optionalBlastResistance.get();
            float effectiveExplosionResistance = this.entity.getBlockExplosionResistance(explosion, world, pos, blockState, fluidState, blastResistance);
            if (effectiveExplosionResistance != blastResistance) {
                return Optional.of(effectiveExplosionResistance);
            }
        }
        return optionalBlastResistance;
    }
}
