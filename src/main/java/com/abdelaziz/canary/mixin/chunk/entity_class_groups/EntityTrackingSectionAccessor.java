package com.abdelaziz.canary.mixin.chunk.entity_class_groups;

import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityTrackingSection.class)
public interface EntityTrackingSectionAccessor<T> {
    @Accessor("collection")
    TypeFilterableList<T> getCollection();
}
