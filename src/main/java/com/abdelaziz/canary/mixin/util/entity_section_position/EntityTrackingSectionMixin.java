package com.abdelaziz.canary.mixin.util.entity_section_position;

import com.abdelaziz.canary.common.entity.tracker.PositionedEntityTrackingSection;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityTrackingSection.class)
public class EntityTrackingSectionMixin implements PositionedEntityTrackingSection {
    private long pos;

    @Override
    public long getPos() {
        return this.pos;
    }

    @Override
    public void setPos(long chunkSectionPos) {
        this.pos = chunkSectionPos;
    }
}
