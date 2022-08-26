package com.abdelaziz.canary.mixin.world.chunk_access;

import com.mojang.datafixers.util.Either;
import com.abdelaziz.canary.common.world.chunk.ChunkHolderExtended;
import net.minecraft.Util;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This patch makes a number of optimizations to chunk retrieval which helps to alleviate some of the slowdown introduced
 * in Minecraft 1.13+.
 * - Scanning the recent request cache is made faster through doing a single linear integer scan. This works through
 * encoding the request's position and status level into a single integer.
 * - Chunk tickets are only created during cache-misses if they were not already created this tick. This prevents the
 * creation of duplicate tickets which would only be immediately discarded after an expensive lookup and sort.
 * - Lambdas are replaced where possible to use simple if-else logic, avoiding allocations and variable captures.
 * - The chunk retrieval logic does not try to begin executing other tasks while blocked unless the future isn't
 * already complete.
 * <p>
 * There are also some organizational and differences which help the JVM to better optimize code here, most of which
 * are documented.
 */
@SuppressWarnings("OverwriteModifiers")
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {
    @Shadow
    @Final
    private ServerChunkCache.MainThreadExecutor mainThreadProcessor;

    @Shadow
    @Final
    private ChunkMap.DistanceManager distanceManager;

    @Shadow
    @Final
    public ChunkMap chunkMap;

    @Shadow
    protected abstract ChunkHolder getChunkHolder(long pos);

    @Shadow
    @Final
    Thread mainThread;

    @Shadow
    protected abstract boolean chunkAbsent(ChunkHolder holder, int maxLevel);

    @Shadow
    abstract boolean runDistanceManagerUpdates();
    
    private long time;
 //runDistanceManagerUpdates
    @Inject(method = "tick()Z", at = @At("HEAD"))
    private void preTick(CallbackInfoReturnable<Boolean> cir) {
        this.time++;
    }

    /**
     * @reason Optimize the function
     * @author JellySquid
     */
    @Overwrite
    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean create) {
        if (Thread.currentThread() != this.mainThread) {
            return this.getChunkOffThread(x, z, status, create);
        }

        // Store a local reference to the cached keys array in order to prevent bounds checks later
        long[] cacheKeys = this.cacheKeys;

        // Create a key which will identify this request in the cache
        long key = createCacheKey(x, z, status);

        for (int i = 0; i < 4; ++i) {
            // Consolidate the scan into one comparison, allowing the JVM to better optimize the function
            // This is considerably faster than scanning two arrays side-by-side
            if (key == cacheKeys[i]) {
                ChunkAccess chunk = this.cacheChunks[i];

                // If the chunk exists for the key or we didn't need to create one, return the result
                if (chunk != null || !create) {
                    return chunk;
                }
            }
        }

        // We couldn't find the chunk in the cache, so perform a blocking retrieval of the chunk from storage
        ChunkAccess chunk = this.getChunkBlocking(x, z, status, create);

        if (chunk != null) {
            this.addToCache(key, chunk);
        } else if (create) {
            throw new IllegalStateException("Chunk not there when requested");
        }

        return chunk;
    }

    private ChunkAccess getChunkOffThread(int x, int z, ChunkStatus status, boolean create) {
        return CompletableFuture.supplyAsync(() -> this.getChunk(x, z, status, create), this.mainThreadProcessor).join();
    }

    /**
     * Retrieves a chunk from the storages, blocking to work on other tasks if the requested chunk needs to be loaded
     * from disk or generated in real-time.
     *
     * @param x      The x-coordinate of the chunk
     * @param z      The z-coordinate of the chunk
     * @param status The minimum status level of the chunk
     * @param create True if the chunk should be loaded/generated if it isn't already, otherwise false
     * @return A chunk if it was already present or loaded/generated by the {@param create} flag
     */
    private ChunkAccess getChunkBlocking(int x, int z, ChunkStatus status, boolean create) {
        final long key = ChunkPos.asLong(x, z);
        final int level = 33 + ChunkStatus.getDistance(status);

        ChunkHolder holder = this.getChunkHolder(key);

        // Check if the holder is present and is at least of the level we need
        if (this.chunkAbsent(holder, level)) {
            if (create) {
                // The chunk holder is missing, so we need to create a ticket in order to load it
                this.createChunkLoadTicket(x, z, level);

                // Tick the chunk manager to have our new ticket processed
                this.runDistanceManagerUpdates();

                // Try to fetch the holder again now that we have requested a load
                holder = this.getChunkHolder(key);

                // If the holder is still not available, we need to fail now... something is wrong.
                if (this.chunkAbsent(holder, level)) {
                    throw Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
                }
            } else {
                // The holder is absent and we weren't asked to create anything, so return null
                return null;
            }
        } else if (((ChunkHolderExtended) holder).updateLastAccessTime(this.time)) {
            // Only create a new chunk ticket if one hasn't already been submitted this tick
            // This maintains vanilla behavior (preventing chunks from being immediately unloaded) while also
            // eliminating the cost of submitting a ticket for most chunk fetches
            this.createChunkLoadTicket(x, z, level);
        }

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> loadFuture = null;
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> statusFuture = ((ChunkHolderExtended) holder).getFutureByStatus(status.getIndex());

        if (statusFuture != null) {
            Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> immediate = statusFuture.getNow(null);

            // If the result is already available, return it
            if (immediate != null) {
                Optional<ChunkAccess> chunk = immediate.left();

                if (chunk.isPresent()) {
                    // Early-return with the already ready chunk
                    return chunk.get();
                }
            } else {
                // The load future will first start with the existing future for this status
                loadFuture = statusFuture;
            }
        }

        // Create a future to load the chunk if none exists
        if (loadFuture == null) {
            if (ChunkHolder.getStatus(holder.getTicketLevel()).isOrAfter(status)) {
                // Create a new future which upgrades the chunk from the previous status level to the desired one
                CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> mergedFuture = this.chunkMap.schedule(holder, status);

                // Add this future to the chunk holder so subsequent calls will see it
                holder.updateChunkToSave(mergedFuture, "schedule chunk status");
                ((ChunkHolderExtended) holder).setFutureForStatus(status.getIndex(), mergedFuture);

                loadFuture = mergedFuture;
            } else {
                if (statusFuture == null) {
                    return null;
                }

                loadFuture = statusFuture;
            }
        }

        // Check if the future is completed first before trying to run other tasks in our idle time
        // This prevents object allocations and method call overhead that would otherwise be instantly invalidated
        // when the future is already complete
        if (!loadFuture.isDone()) {
            // Perform other chunk tasks while waiting for this future to complete
            // This returns when either the future is done or there are no other tasks remaining
            this.mainThreadProcessor.managedBlock(loadFuture::isDone);

            /*
            * if (!loadFuture.isDone()) {
            ServerChunkCache.MainThreadExecutor var10000 = this.f_8332_;
            Objects.requireNonNull(loadFuture);
            var10000.m_18701_(loadFuture::isDone);
        }
            *
            * */
        }

        // Wait for the result of the future and unwrap it, returning null if the chunk is absent
        return loadFuture.join().left().orElse(null);
    }

    private void createChunkLoadTicket(int x, int z, int level) {
        ChunkPos chunkPos = new ChunkPos(x, z);

        this.distanceManager.addTicket(TicketType.UNKNOWN, chunkPos, level, chunkPos);
    }

    /**
     * The array of keys (encoding positions and status levels) for the recent lookup cache
     */
    private final long[] cacheKeys = new long[4];

    /**
     * The array of values associated with each key in the recent lookup cache.
     */
    private final ChunkAccess[] cacheChunks = new ChunkAccess[4];

    /**
     * Encodes a chunk position and status into a long. Uses 28 bits for each coordinate value, and 8 bits for the
     * status.
     */
    private static long createCacheKey(int chunkX, int chunkZ, ChunkStatus status) {
        return ((long) chunkX & 0xfffffffL) | (((long) chunkZ & 0xfffffffL) << 28) | ((long) status.getIndex() << 56);
    }

    /**
     * Prepends the chunk with the given key to the recent lookup cache
     */
    private void addToCache(long key, ChunkAccess chunk) {
        for (int i = 3; i > 0; --i) {
            this.cacheKeys[i] = this.cacheKeys[i - 1];
            this.cacheChunks[i] = this.cacheChunks[i - 1];
        }

        this.cacheKeys[0] = key;
        this.cacheChunks[0] = chunk;
    }

    /**
     * Reset our own caches whenever vanilla does the same
     */
    @Inject(method = "initChunkCaches()V", at = @At("HEAD"))
    private void onCachesCleared(CallbackInfo ci) {
        Arrays.fill(this.cacheKeys, Long.MAX_VALUE);
        Arrays.fill(this.cacheChunks, null);
    }
}