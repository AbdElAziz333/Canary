package com.abdelaziz.canary.common.util.constants;

import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;

/**
 * Pre-initialized constants to avoid unnecessary allocations.
 */
public interface RaidConstants {
    // The call to Raid#getLeaderBannerInstance() is very expensive, so cache it and re-use it during AI ticking
   ItemStack CACHED_OMINOUS_BANNER = Raid.getLeaderBannerInstance();
}
