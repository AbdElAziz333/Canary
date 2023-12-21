package com.abdelaziz.canary.mixin.util.block_tracking.block_listening;

import com.abdelaziz.canary.common.entity.block_tracking.SectionedBlockChangeTracker;
import com.abdelaziz.canary.common.util.deduplication.CanaryInterner;
import com.abdelaziz.canary.common.util.deduplication.CanaryInternerWrapper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public class LevelMixin implements CanaryInternerWrapper<SectionedBlockChangeTracker> {
    private final CanaryInterner<SectionedBlockChangeTracker> blockChangeTrackers = new CanaryInterner<>();

    @Override
    public SectionedBlockChangeTracker getCanonical(SectionedBlockChangeTracker value) {
        return this.blockChangeTrackers.getCanonical(value);
    }

    @Override
    public void deleteCanonical(SectionedBlockChangeTracker value) {
        this.blockChangeTrackers.deleteCanonical(value);
    }
}