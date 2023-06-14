package com.abdelaziz.canary.mixin.world.player_chunk_tick;

import com.abdelaziz.canary.common.util.Pos;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.server.level.ChunkMap.isChunkInRange;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    protected abstract void playerLoadedChunk(ServerPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> cachedDataPacket, LevelChunk chunk);

    @Shadow
    protected abstract SectionPos updatePlayerPos(ServerPlayer player);

    /**
     * @author JellySquid
     * @reason Defer sending chunks to the player so that we can batch them together
     */
    @Overwrite
    public void move(ServerPlayer player) {
        for (ChunkMap.TrackedEntity tracker : this.entityMap.values()) {
            if (tracker.entity == player) {
                tracker.updatePlayers(this.level.players());
            } else {
                tracker.updatePlayer(player);
            }
        }

        SectionPos oldPos = player.getLastSectionPos();
        SectionPos newPos = SectionPos.of(player);

        boolean isWatchingWorld = this.playerMap.ignored(player);
        boolean doesNotGenerateChunks = this.skipPlayer(player);
        boolean movedSections = !newPos.equals(oldPos);

        if (movedSections || isWatchingWorld != doesNotGenerateChunks) {
            // Notify the client that the chunk map origin has changed. This must happen before any chunk payloads are sent.
            this.updatePlayerPos(player);

            if (!isWatchingWorld) {
                this.distanceManager.removePlayer(oldPos, player);
            }

            if (!doesNotGenerateChunks) {
                this.distanceManager.addPlayer(newPos, player);
            }

            if (!isWatchingWorld && doesNotGenerateChunks) {
                this.playerMap.ignorePlayer(player);
            }

            if (isWatchingWorld && !doesNotGenerateChunks) {
                this.playerMap.unIgnorePlayer(player);
            }

            long oldChunkPos = ChunkPos.asLong(oldPos.getX(), oldPos.getZ());
            long newChunkPos = ChunkPos.asLong(newPos.getX(), newPos.getZ());

            this.playerMap.updatePlayer(oldChunkPos, newChunkPos, player);
        } else {
            // The player hasn't changed locations and isn't changing dimensions
            return;
        }

        // We can only send chunks if the world matches. This hoists a check that
        // would otherwise be performed every time we try to send a chunk over.
        if (player.level() == this.level) {
            this.sendChunks(oldPos, player);
        }
    }

    private void sendChunks(SectionPos oldPos, ServerPlayer player) {
        int newCenterX = Pos.ChunkCoord.fromBlockCoord(Mth.floor(player.getX()));
        int newCenterZ = Pos.ChunkCoord.fromBlockCoord(Mth.floor(player.getZ()));

        int oldCenterX = oldPos.x();
        int oldCenterZ = oldPos.z();

        int watchRadius = this.viewDistance;
        int watchRadiusIncr = watchRadius + 1;
        int watchDiameter = watchRadius * 2;

        if (Math.abs(oldCenterX - newCenterX) <= watchDiameter && Math.abs(oldCenterZ - newCenterZ) <= watchDiameter) {
            int minX = Math.min(newCenterX, oldCenterX) - watchRadiusIncr;
            int minZ = Math.min(newCenterZ, oldCenterZ) - watchRadiusIncr;
            int maxX = Math.max(newCenterX, oldCenterX) + watchRadiusIncr;
            int maxZ = Math.max(newCenterZ, oldCenterZ) + watchRadiusIncr;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean isWithinOldRadius = isChunkInRange(x, z, oldCenterX, oldCenterZ, watchRadius);
                    boolean isWithinNewRadius = isChunkInRange(x, z, newCenterX, newCenterZ, watchRadius);

                    if (isWithinNewRadius && !isWithinOldRadius) {
                        this.startWatchingChunk(player, x, z);
                    }

                    if (isWithinOldRadius && !isWithinNewRadius) {
                        this.stopWatchingChunk(player, x, z);
                    }
                }
            }
        } else {
            for (int x = oldCenterX - watchRadiusIncr; x <= oldCenterX + watchRadiusIncr; ++x) {
                for (int z = oldCenterZ - watchRadiusIncr; z <= oldCenterZ + watchRadiusIncr; ++z) {
                    if (isChunkInRange(x, z, oldCenterX, oldCenterZ, watchRadius)) {
                        this.stopWatchingChunk(player, x, z);
                    }
                }
            }

            for (int x = newCenterX - watchRadiusIncr; x <= newCenterX + watchRadiusIncr; ++x) {
                for (int z = newCenterZ - watchRadiusIncr; z <= newCenterZ + watchRadiusIncr; ++z) {
                    if (isChunkInRange(x, z, newCenterX, newCenterZ, watchRadius)) {
                        this.startWatchingChunk(player, x, z);
                    }
                }
            }
        }
    }

    protected void startWatchingChunk(ServerPlayer player, int x, int z) {
        ChunkHolder holder = this.getVisibleChunkIfPresent(ChunkPos.asLong(x, z));

        if (holder != null) {
            LevelChunk chunk = holder.getTickingChunk();

            if (chunk != null) {
                this.playerLoadedChunk(player, new MutableObject<>(), chunk);
            }
        }
    }

    protected void stopWatchingChunk(ServerPlayer player, int x, int z) {
        player.untrackChunk(new ChunkPos(x, z));
    }

    @Shadow
    @Final
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    @Final
    private PlayerMap playerMap;

    @Shadow
    @Final
    private ChunkMap.DistanceManager distanceManager;

    @Shadow
    int viewDistance;

    @Shadow
    protected abstract boolean skipPlayer(ServerPlayer player);

    @Shadow
    protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);
}
