package com.abdelaziz.canary.mixin.entity.skip_fire_check;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    private int remainingFireTicks;

    @Shadow
    protected abstract int getFireImmuneTicks();

    @Shadow
    public boolean wasOnFire;

    @Shadow
    public boolean isInPowderSnow;

    @Shadow
    public abstract boolean isInWaterRainOrBubble();

    @Redirect(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockStatesIfLoaded(Lnet/minecraft/world/phys/AABB;)Ljava/util/stream/Stream;"
            )
    )
    private Stream<BlockState> skipFireTestIfResultDoesNotMatter(Level world, AABB box) {
        // Skip scanning the blocks around the entity touches by returning an empty stream when the result does not matter
        if ((this.remainingFireTicks > 0 || this.remainingFireTicks == -this.getFireImmuneTicks()) && (!this.wasOnFire || !this.isInPowderSnow && !this.isInWaterRainOrBubble())) {
            return null;
        }

        return world.getBlockStatesIfLoaded(box);
    }

    @Redirect(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;noneMatch(Ljava/util/function/Predicate;)Z"
            )
    )
    private boolean skipNullStream(Stream<BlockState> stream, Predicate<BlockState> predicate) {
        if (stream == null) {
            return true;
        }
        return stream.noneMatch(predicate);
    }
}
