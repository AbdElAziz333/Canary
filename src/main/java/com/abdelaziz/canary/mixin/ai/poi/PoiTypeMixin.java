package com.abdelaziz.canary.mixin.ai.poi;

import com.abdelaziz.canary.common.world.interests.types.PoiTypeHelper;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

/**
 * Replaces the backing map type with a faster collection type which uses reference equality.
 */
@Mixin(PoiType.class)
public class PoiTypeMixin {
    @Shadow
    @Final
    @Mutable
    private static Map<BlockState, PoiType> TYPE_BY_STATE;

    static {
        //TYPE_BY_STATE = new Reference2ReferenceOpenHashMap<>(TYPE_BY_STATE);

        PoiTypeHelper.init(TYPE_BY_STATE.keySet());
    }
}