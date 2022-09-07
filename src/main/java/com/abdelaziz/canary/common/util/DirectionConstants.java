package com.abdelaziz.canary.common.util;

import net.minecraft.util.math.Direction;

/**
 * Pre-initialized constants to avoid unnecessary allocations.
 */
public final class DirectionConstants {
    public static final Direction[] ALL = Direction.values();
    public static final Direction[] VERTICAL = {Direction.DOWN, Direction.UP};
    public static final Direction[] HORIZONTAL = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
    private DirectionConstants() {}
}
