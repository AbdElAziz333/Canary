package com.abdelaziz.canary.common.world.interests;

import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface PointOfInterestSetExtended {
    void collectMatchingPoints(Predicate<RegistryEntry<PointOfInterestType>> type, PointOfInterestStorage.OccupationStatus status,
                               Consumer<PointOfInterest> consumer);
}