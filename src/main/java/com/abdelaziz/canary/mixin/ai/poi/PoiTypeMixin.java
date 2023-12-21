package com.abdelaziz.canary.mixin.ai.poi;

import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Replaces the backing map type with a faster collection type which uses reference equality.
 */
@Mixin(PoiType.class)
public class PoiTypeMixin {
    @Accessor("TYPE_BY_STATE")
    public static Map<BlockState, PoiType> getBlockStateToPoiType() {
        throw new UnsupportedOperationException("Replaced by Mixin");
    }

    @Accessor("TYPE_BY_STATE")
    public static void setBlockStateToPoiType(Map<BlockState, PoiType> newMap) {
        throw new UnsupportedOperationException("Replaced by Mixin");
    }
}