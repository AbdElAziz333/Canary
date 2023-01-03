package com.abdelaziz.canary.mixin.ai.poi;

import com.abdelaziz.canary.common.world.interests.types.PoiTypeHelper;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(PoiType.class)
public class PoiTypeMixin {
    @Mutable
    @Shadow
    @Final
    private static Map<BlockState, PoiType> TYPE_BY_STATE;

    static {
        TYPE_BY_STATE = new Reference2ReferenceOpenHashMap<>(TYPE_BY_STATE);

        PoiTypeHelper.setup(TYPE_BY_STATE.keySet());
    }
}
