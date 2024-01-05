package com.abdelaziz.canary.mixin.chunk.replace_streams;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin {
    @Shadow @Final private ChunkHolder.PlayerProvider playerProvider;

    @Shadow @Final
    ChunkPos pos;

    /**
     * @reason Avoid streams.
     * @author AbdElAziz
     * */
    @Overwrite
    private void broadcast(Packet<?> packet, boolean value) {
        for (ServerPlayer player : this.playerProvider.getPlayers(this.pos, value)) {
            player.connection.send(packet);
        }
    }
}
