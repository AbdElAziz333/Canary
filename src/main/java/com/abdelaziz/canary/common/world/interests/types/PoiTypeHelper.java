package com.abdelaziz.canary.common.world.interests.types;

import com.abdelaziz.canary.common.Canary;
import com.abdelaziz.canary.common.util.collections.SetFactory;
import com.abdelaziz.canary.mixin.ai.poi.PoiTypesMixin;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

import java.util.Map;
import java.util.Set;

/**
 * Replaces the type of the blockstate to POI map with a faster collection type which uses reference equality.
 */
@Mod.EventBusSubscriber(modid = Canary.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PoiTypeHelper {
    private static Set<BlockState> TYPES;

    public static boolean shouldScan(LevelChunkSection section) {
        return section.maybeHas(TYPES::contains);
    }

    @SubscribeEvent
    public static void setup(FMLLoadCompleteEvent ev) {
        if (!EnabledMarker.class.isAssignableFrom(PoiManager.class)) {
            return;
        }
        ev.enqueueWork(() -> {
            if (TYPES != null) {
                throw new IllegalStateException("Already initialized");
            }
            Map<BlockState, PoiType> blockstatePOIMap = PoiTypesMixin.getBlockStateToPoiType();
            blockstatePOIMap = new Reference2ReferenceOpenHashMap<>(blockstatePOIMap);
            PoiTypesMixin.setBlockStateToPoiType(blockstatePOIMap);

            TYPES = SetFactory.createFastRefBasedCopy(blockstatePOIMap.keySet());
        }).exceptionally(throwable -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new RuntimeException(throwable);
        });
    }

    public interface EnabledMarker {
    }
}