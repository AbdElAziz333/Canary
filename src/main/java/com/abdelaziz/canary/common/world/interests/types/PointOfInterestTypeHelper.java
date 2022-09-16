package com.abdelaziz.canary.common.world.interests.types;

import com.abdelaziz.canary.common.Canary;
import com.abdelaziz.canary.common.util.collections.SetFactory;
import com.abdelaziz.canary.mixin.ai.poi.PointOfInterestTypeMixin;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Map;
import java.util.Set;

/**
 * Replaces the type of the blockstate to POI map with a faster collection type which uses reference equality.
 */
@Mod.EventBusSubscriber(modid = Canary.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PointOfInterestTypeHelper {
    private static Set<BlockState> TYPES;

    public static boolean shouldScan(ChunkSection section) {
        return section.hasAny(TYPES::contains);
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent ev) {
        if (!EnabledMarker.class.isAssignableFrom(PointOfInterestStorage.class)) {
            return;
        }
        if (TYPES != null) {
            throw new IllegalStateException("Already initialized");
        }
        Map<BlockState, PointOfInterestType> blockstatePOIMap = PointOfInterestTypeMixin.getBlockStateToPointOfInterestType();
        blockstatePOIMap = new Reference2ReferenceOpenHashMap<>(blockstatePOIMap);
        PointOfInterestTypeMixin.setBlockStateToPointOfInterestType(blockstatePOIMap);

        TYPES = SetFactory.createFastRefBasedCopy(blockstatePOIMap.keySet());
    }

    public interface EnabledMarker {
    }
}