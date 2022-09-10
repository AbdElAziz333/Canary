package com.abdelaziz.canary.mixin.ai.poi;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import com.abdelaziz.canary.common.world.interests.types.PointOfInterestTypeHelper;
import net.minecraft.block.BlockState;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Replaces the backing map type with a faster collection type which uses reference equality.
 */
@Mixin(PointOfInterestTypes.class)
public class PointOfInterestTypesMixin {
    @Accessor("POI_STATES_TO_TYPE")
    public static Map<BlockState, PointOfInterestType> getBlockStateToPointOfInterestType() {
        throw new UnsupportedOperationException("Replaced by Mixin");
    }

    @Accessor("POI_STATES_TO_TYPE")
    public static void setBlockStateToPointOfInterestType(Map<BlockState, PointOfInterestType> newMap) {
        throw new UnsupportedOperationException("Replaced by Mixin");
    }
}
