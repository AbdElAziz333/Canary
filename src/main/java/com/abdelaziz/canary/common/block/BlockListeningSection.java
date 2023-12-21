package com.abdelaziz.canary.common.block;

import com.abdelaziz.canary.common.entity.block_tracking.SectionedBlockChangeTracker;

public interface BlockListeningSection {
    void addToCallback(ListeningBlockStatePredicate blockGroup, SectionedBlockChangeTracker tracker);
    void removeFromCallback(ListeningBlockStatePredicate blockGroup, SectionedBlockChangeTracker tracker);
}