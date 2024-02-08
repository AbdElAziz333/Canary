package com.abdelaziz.canary.mixin.ai.raid;

import com.abdelaziz.canary.common.util.constants.RaidConstants;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Raider.ObtainRaidLeaderBannerGoal.class)
public class ObtainRaidLeaderBannerGoalMixin {
    @Redirect(
            method = "canUse()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/raid/Raid;getLeaderBannerInstance()Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack getOminousBanner() {
        return RaidConstants.CACHED_OMINOUS_BANNER;
    }
}