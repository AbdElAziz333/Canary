package com.abdelaziz.canary.mixin.world.block_entity_ticking.sleeping;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public class WorldMixin {

    @Redirect(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;shouldTickBlocksInChunk(J)Z")
    )
    private boolean shouldTickBlockPosFilterNull(World instance, long pos) {
        if (pos == 0) {
            return false;
        }
        return instance.shouldTickBlocksInChunk(pos);
    }
}