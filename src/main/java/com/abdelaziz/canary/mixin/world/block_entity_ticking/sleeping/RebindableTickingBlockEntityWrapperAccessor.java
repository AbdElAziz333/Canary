package com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping;

import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net/minecraft/world/level/chunk/LevelChunk$RebindableTickingBlockEntityWrapper")
public interface RebindableTickingBlockEntityWrapperAccessor {
    @Invoker
    void callRebind(TickingBlockEntity wrapped);

    @Accessor
    TickingBlockEntity getTicker();
}
