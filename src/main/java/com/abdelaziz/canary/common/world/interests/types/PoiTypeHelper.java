package com.abdelaziz.canary.common.world.interests.types;

import com.abdelaziz.canary.common.Canary;
import com.abdelaziz.canary.common.util.collections.SetFactory;
import com.abdelaziz.canary.mixin.ai.poi.PoiTypeMixin;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

//@Mod.EventBusSubscriber(modid = Canary.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PoiTypeHelper {
    private static Set<BlockState> TYPES;

    public static boolean shouldScan(LevelChunkSection section) {
        return section.maybeHas(TYPES::contains);
    }

    //@SubscribeEvent
    public static void setup(FMLCommonSetupEvent ev) {
        if (!EnabledMarker.class.isAssignableFrom(PoiManager.class)) {
            return;
        }
        if (TYPES != null) {
            throw new IllegalStateException("Already initialized");
        }
        Map<BlockState, PoiType> blockstatePOIMap = PoiTypeMixin.getBlockStateToPoiType();
        blockstatePOIMap = new Reference2ReferenceOpenHashMap<>(blockstatePOIMap);
        PoiTypeMixin.setBlockStateToPoiType(blockstatePOIMap);

        TYPES = SetFactory.createFastRefBasedCopy(blockstatePOIMap.keySet());
    }

    public interface EnabledMarker {
    }
}