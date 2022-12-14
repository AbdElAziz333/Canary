package com.abdelaziz.canary.mixin.ai.raid;

import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Raider.ObtainRaidLeaderBannerGoal.class)
public class ObtainRaidLeaderBannerGoalMixin {
    // The call to Raid#getLeaderBannerInstance() is very expensive, so cache it and re-use it during AI ticking
    private static final ItemStack CACHED_OMINOUS_BANNER = Raid.getLeaderBannerInstance();

    @Redirect(
            method = "canUse()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/raid/Raid;getLeaderBannerInstance()Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack getOminousBanner() {
        return CACHED_OMINOUS_BANNER;
    }
}