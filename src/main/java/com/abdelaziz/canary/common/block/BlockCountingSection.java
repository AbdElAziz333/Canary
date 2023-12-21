package com.abdelaziz.canary.common.block;

public interface BlockCountingSection {
    boolean mayContainAny(TrackedBlockStatePredicate trackedBlockStatePredicate);
}