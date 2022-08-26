package com.abdelaziz.canary.common.world.interests.iterator;

import net.minecraft.world.entity.ai.village.poi.PoiType;

import java.util.function.Predicate;

public record SinglePointOfInterestTypeFilter(PoiType type) implements Predicate<PoiType> {

    @Override
    public boolean test(PoiType other) {
        return this.type == other;
    }

    public PoiType getType() {
        return this.type;
    }
}
