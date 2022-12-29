package com.abdelaziz.canary.mixin.ai.poi.tasks;

import com.abdelaziz.canary.common.util.POIRegistryEntries;
import com.abdelaziz.canary.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.behavior.LocateHidingPlace;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(LocateHidingPlace.class)
public class LocateHidingPlaceMixin {

    @Redirect(
            method = "lambda$create$5",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;getRandom(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;)Ljava/util/Optional;",
                    remap = true
            )
    )
    private static Optional<BlockPos> useFasterPOILookup(PoiManager poiManager, Predicate<Holder<PoiType>> typePredicate, Predicate<BlockPos> positionPredicate, PoiManager.Occupancy occupationStatus, BlockPos pos, int radius, RandomSource random) {
        return poiManager.getRandom(new SinglePointOfInterestTypeFilter(POIRegistryEntries.HOME_ENTRY), positionPredicate, occupationStatus, pos, radius, random);
    }
}
