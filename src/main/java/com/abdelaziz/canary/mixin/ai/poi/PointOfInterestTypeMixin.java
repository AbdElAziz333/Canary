package com.abdelaziz.canary.mixin.ai.poi;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import com.abdelaziz.canary.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import com.abdelaziz.canary.common.world.interests.types.PointOfInterestTypeHelper;
import net.minecraft.block.BlockState;
import net.minecraft.world.poi.PointOfInterestType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Replaces the backing map type with a faster collection type which uses reference equality.
 */
@Mixin(PointOfInterestType.class)
public class PointOfInterestTypeMixin {
    @Accessor("BLOCK_STATE_TO_POINT_OF_INTEREST_TYPE")
    public static Map<BlockState, PointOfInterestType> getBlockStateToPointOfInterestType() {
        throw new UnsupportedOperationException("Replaced by Mixin");
    }

    @Accessor("BLOCK_STATE_TO_POINT_OF_INTEREST_TYPE")
    public static void setBlockStateToPointOfInterestType(Map<BlockState, PointOfInterestType> newMap) {
        throw new UnsupportedOperationException("Replaced by Mixin");
    }
}