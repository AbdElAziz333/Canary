package com.abdelaziz.canary.mixin.math.fast_util;

import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(Direction.class)
public class DirectionMixin {
    @Shadow
    @Final
    private static Direction[] VALUES;

    @Shadow
    @Final
    private int oppositeIndex;

    /**
     * @reason Avoid the modulo/abs operations
     * @author JellySquid
     */
    @Overwrite
    public Direction getOpposite() {
        return VALUES[this.oppositeIndex];
    }

    /**
     * @reason Do not allocate an excessive number of Direction arrays
     * @author JellySquid
     */
    @Overwrite
    public static Direction getRandom(Random rand) {
        return VALUES[rand.nextInt(VALUES.length)];
    }
}
