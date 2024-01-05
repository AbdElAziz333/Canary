package com.abdelaziz.canary.mixin.chunk.replace_streams;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Mixin(DistanceManager.class)
public class DistanceManagerMixin {
    @Shadow @Final
    Executor mainThreadExecutor;

    @Redirect(
            method = "runAllUpdates",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;forEach(Ljava/util/function/Consumer;)V"
            )
    )
    private void replaceStreams(Set<ChunkHolder> set, Consumer<?> consumer, ChunkMap map) {
        for (ChunkHolder chunk : set) {
            chunk.updateFutures(map, this.mainThreadExecutor);
        }
    }
}
