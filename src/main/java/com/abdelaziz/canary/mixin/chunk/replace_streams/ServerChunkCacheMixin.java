package com.abdelaziz.canary.mixin.chunk.replace_streams;

import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
    @Redirect(
            method = "tickChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"
            )
    )
    private void replaceStreams(List<ServerChunkCache.ChunkAndHolder> instance, Consumer<?> consumer) {
        for (ServerChunkCache.ChunkAndHolder chunk : instance) {
            chunk.holder().broadcastChanges(chunk.chunk());
        }
    }
}
