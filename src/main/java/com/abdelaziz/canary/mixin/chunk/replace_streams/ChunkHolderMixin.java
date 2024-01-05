package com.abdelaziz.canary.mixin.chunk.replace_streams;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin {
    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    private void broadcast(List<ServerPlayer> list, Packet<?> packet) {
        for (ServerPlayer player : list) {
            player.connection.send(packet);
        }
    }
}
