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
            method = "lambda$create$5",//lambda$create$5 - m_275799_
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;getRandom(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;)Ljava/util/Optional;"
            )
    )
    private static Optional<BlockPos> useFasterPOILookup(PoiManager poiManager, Predicate<Holder<PoiType>> typePredicate, Predicate<BlockPos> positionPredicate, PoiManager.Occupancy occupationStatus, BlockPos pos, int radius, RandomSource random) {
        return poiManager.getRandom(new SinglePointOfInterestTypeFilter(POIRegistryEntries.HOME_ENTRY), positionPredicate, occupationStatus, pos, radius, random);
    }

    @Redirect(
            method = "lambda$create$8",//lambda$create$8 - m_275799_
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;find(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;"
            )
    )
    private static Optional<BlockPos> useFasterPOILookup(PoiManager pointOfInterestStorage, Predicate<Holder<PoiType>> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int radius, PoiManager.Occupancy occupationStatus) {
        return pointOfInterestStorage.find(new SinglePointOfInterestTypeFilter(POIRegistryEntries.HOME_ENTRY), posPredicate, pos, radius, occupationStatus);
    }
}
