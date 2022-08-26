package com.abdelaziz.canary.common.world.interests;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface PointOfInterestSetExtended {
    void collectMatchingPoints(Predicate<Holder<PoiType>> type, PoiManager.Occupancy status,
                               Consumer<PoiRecord> consumer);
}