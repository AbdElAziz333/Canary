package com.abdelaziz.canary.common.world.interests.iterator;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;

import java.util.function.Predicate;

public record SinglePointOfInterestTypeFilter(
        Holder<PoiType> type) implements Predicate<Holder<PoiType>> {

    @Override
    public boolean test(Holder<PoiType> other) {
        return this.type == other;
    }

    public Holder<PoiType> getType() {
        return this.type;
    }
}
