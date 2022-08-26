package com.abdelaziz.canary.mixin.ai.poi.tasks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

@Mixin(targets = "net.minecraft.world.entity.raid.Raider$AttackHomeGoal")
public class RaiderEntityAttackHomeGoalMixin {

    @Redirect(
            method = "tryFindHome()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;getRandom(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;Lnet/minecraft/core/BlockPos;ILjava/util/Random;)Ljava/util/Optional;"
            )
    )
    private Optional<BlockPos> redirect(PoiManager instance, Predicate<Holder<PoiType>> typePredicate, Predicate<BlockPos> positionPredicate, PoiManager.Occupancy occupationStatus, BlockPos pos, int radius, Random random) {
        return instance.getRandom(PoiType.HOME.getPredicate(), positionPredicate, occupationStatus, pos, radius, random);
    }
}