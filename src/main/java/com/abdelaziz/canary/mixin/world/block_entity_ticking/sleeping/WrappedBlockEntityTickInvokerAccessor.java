package com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping;

import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net/minecraft/world/chunk/LevelChunk$RebindableTickingBlockEntityWrapper" )
public interface WrappedBlockEntityTickInvokerAccessor {
    @Invoker
    void callSetWrapped(TickingBlockEntity wrapped);

    @Accessor
    TickingBlockEntity getWrapped();
}
