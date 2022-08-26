package com.abdelaziz.canary.common.util;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

public class POIRegistryEntries {
    //Using a separate class, so the registry lookup happens after the registry is initialized
    public static final Holder<PoiType> NETHER_PORTAL_ENTRY = Registry.POINT_OF_INTEREST_TYPE.getHolderOrThrow(PoiTypes.NETHER_PORTAL);
    public static final Holder<PoiType> HOME_ENTRY = Registry.POINT_OF_INTEREST_TYPE.getHolderOrThrow(PoiTypes.HOME);
}
