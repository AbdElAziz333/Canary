package com.abdelaziz.canary.common.util.constants;

/**
 * Pre-initialized constants to avoid unnecessary allocations.
 */
public final class ArrayConstants {
    private ArrayConstants() {}

    public static final int[] EMPTY = new int[0];
    public static final int[] ZERO = new int[]{0};
}
