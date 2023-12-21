package com.abdelaziz.canary.mixin.util.entity_section_position;

import com.abdelaziz.canary.common.entity.PositionedEntityTrackingSection;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntitySection.class)
public class EntitySectionMixin implements PositionedEntityTrackingSection {
    private long pos;

    @Override
    public void setPos(long chunkSectionPos) {
        this.pos = chunkSectionPos;
    }

    @Override
    public long getPos() {
        return this.pos;
    }
}
