package com.abdelaziz.canary.common.block;

import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public abstract class TrackedBlockStatePredicate implements Predicate<BlockState> {
    public static final AtomicBoolean FULLY_INITIALIZED;

    static {
        FULLY_INITIALIZED = new AtomicBoolean(false);
        if (!BlockStateFlags.ENABLED) { //classload the BlockStateFlags class which initializes the content of ALL_FLAGS
            System.out.println("Canary Cached BlockState Flags are disabled!");
        }
    }

    private final int index;

    public TrackedBlockStatePredicate(int index) {
        if (FULLY_INITIALIZED.get()) {
            throw new IllegalStateException("Canary Cached BlockState Flags: Cannot register more flags after assuming to be fully initialized.");
        }
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}
