package com.abdelaziz.canary.common.block;

public interface BlockCountingSection {
    boolean anyMatch(TrackedBlockStatePredicate trackedBlockStatePredicate);
}
