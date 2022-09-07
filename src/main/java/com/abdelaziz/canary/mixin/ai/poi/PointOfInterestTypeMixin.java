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
    @Mutable
    @Shadow
    @Final
    private static Map<BlockState, PointOfInterestType> BLOCK_STATE_TO_POINT_OF_INTEREST_TYPE;

    static {
        BLOCK_STATE_TO_POINT_OF_INTEREST_TYPE = new Reference2ReferenceOpenHashMap<>(BLOCK_STATE_TO_POINT_OF_INTEREST_TYPE);

        PointOfInterestTypeHelper.init(BLOCK_STATE_TO_POINT_OF_INTEREST_TYPE.keySet());
    }

    @Mutable
    @Shadow
    @Final
    private Predicate<PointOfInterestType> completionCondition;

    @Inject(
            method = "<init>(Ljava/lang/String;Ljava/util/Set;II)V",
            at = @At(value = "RETURN")
    )
    private void initCompletionPredicate(String id, Set<?> blockStates, int ticketCount, int searchDistance, CallbackInfo ci) {
        this.completionCondition = new SinglePointOfInterestTypeFilter((PointOfInterestType) (Object) this);
    }
}
