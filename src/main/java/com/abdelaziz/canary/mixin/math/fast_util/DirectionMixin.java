package com.abdelaziz.canary.mixin.math.fast_util;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Direction.class)
public class DirectionMixin {
    @Shadow
    @Final
    private static Direction[] ALL;

    @Shadow
    @Final
    private int idOpposite;

    /**
     * @reason Avoid the modulo/abs operations
     * @author JellySquid
     */
    @Overwrite
    public Direction getOpposite() {
        return ALL[this.idOpposite];
    }

    /**
     * @reason Do not allocate an excessive number of Direction arrays
     * @author JellySquid
     */
    @Overwrite
    public static Direction random(RandomSource rand) {
        return ALL[rand.nextInt(ALL.length)];
    }
}
